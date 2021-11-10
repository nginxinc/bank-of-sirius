# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Userservice manages user account creation, user login, and related tasks
"""

import atexit
from datetime import datetime, timedelta
import logging
import os
from pathlib import Path

import sys
import re

import bcrypt
import jwt
from flask import Flask, jsonify, request
import bleach
from sqlalchemy.exc import OperationalError, SQLAlchemyError
from db import UserDb

from opentelemetry import trace

import tracing
from tracing import OpenTelemetryConfiguration
from flask_management_endpoints import ManagementEndpoints

APP_NAME = 'userservice'


def create_app():
    """Flask application factory to create instances
    of the Userservice Flask App
    """
    app = Flask('userservice')

    @app.route('/users', methods=['POST'])
    def create_user():
        """Create a user record.

        Fails if that username already exists.

        Generates a unique accountid.

        request fields:
        - username
        - password
        - password-repeat
        - firstname
        - lastname
        - birthday
        - timezone
        - address
        - state
        - zip
        - ssn
        """
        span = trace.get_current_span()
        try:
            app.logger.debug('Sanitizing input')
            req = {k: bleach.clean(v) for k, v in request.form.items()}

            if 'username' in req:
                span.set_attribute('username', req['username'])

            __validate_new_user(req)
            # Check if user already exists
            if users_db.get_user(req['username']) is not None:
                raise NameError(f"user {req['username']} already exists")

            # Create password hash with salt
            app.logger.debug("Creating password hash.")
            password = req['password']
            salt = bcrypt.gensalt()
            passhash = bcrypt.hashpw(password.encode('utf-8'), salt)

            accountid = users_db.generate_accountid()

            # Create user data to be added to the database
            user_data = {
                'accountid': accountid,
                'username': req['username'],
                'passhash': passhash,
                'firstname': req['firstname'],
                'lastname': req['lastname'],
                'birthday': req['birthday'],
                'timezone': req['timezone'],
                'address': req['address'],
                'state': req['state'],
                'zip': req['zip'],
                'ssn': req['ssn'],
            }
            # Add user_data to database
            app.logger.debug("Adding user to the database")
            users_db.add_user(user_data)
            app.logger.info("Successfully created user")

        except UserWarning as warn:
            app.logger.error("Error creating new user: %s", str(warn))
            span.add_event(str(warn))
            return str(warn), 400
        except NameError as err:
            app.logger.error("Error creating new user: %s", str(err))
            span.record_exception(err)
            return str(err), 409
        except SQLAlchemyError as err:
            app.logger.error("Error creating new user: %s", str(err))
            span.record_exception(err)
            return 'failed to create user', 500

        return jsonify({}), 201

    def __validate_new_user(req):
        app.logger.debug('validating create user request: %s', str(req))
        # Check if required fields are filled
        fields = (
            'username',
            'password',
            'password-repeat',
            'firstname',
            'lastname',
            'birthday',
            'timezone',
            'address',
            'state',
            'zip',
            'ssn',
        )
        if any(f not in req for f in fields):
            raise UserWarning('missing required field(s)')
        if any(not bool(req[f] or req[f].strip()) for f in fields):
            raise UserWarning('missing value for input field(s)')

        # Verify username contains only 2-15 alphanumeric or underscore characters
        if not re.match(r"\A[a-zA-Z0-9_]{2,15}\Z", req['username']):
            raise UserWarning('username must contain 2-15 alphanumeric characters or underscores')
        # Check if passwords match
        if not req['password'] == req['password-repeat']:
            raise UserWarning('passwords do not match')

    @app.route('/login', methods=['GET'])
    def login():
        """Login a user and return a JWT token

        Fails if username doesn't exist or password doesn't match hash

        token expiry time determined by environment variable

        request fields:
        - username
        - password
        """
        span = trace.get_current_span()
        app.logger.debug('Sanitizing login input')
        username = bleach.clean(request.args.get('username'))
        span.set_attribute('username', username)
        password = bleach.clean(request.args.get('password'))

        # Get user data
        try:
            app.logger.debug('Getting the user data')
            user = users_db.get_user(username)
            if user is None:
                raise LookupError(f"user {username} does not exist")

            # Validate the password
            app.logger.debug('Validating the password')
            if not bcrypt.checkpw(password.encode('utf-8'), user['passhash']):
                raise PermissionError('invalid login')

            full_name = f"{user['firstname']} {user['lastname']}"
            exp_time = datetime.utcnow() + timedelta(seconds=app.config['EXPIRY_SECONDS'])
            payload = {
                'user': username,
                'acct': user['accountid'],
                'name': full_name,
                'iat': datetime.utcnow(),
                'exp': exp_time,
            }
            app.logger.debug('Creating jwt token.')
            token = jwt.encode(payload, app.config['PRIVATE_KEY'], algorithm='RS256')
            app.logger.info('Login Successful')
            return jsonify({'token': token.decode("utf-8")}), 200

        except LookupError as err:
            app.logger.error('Error logging in: %s', str(err))
            span.record_exception(err)
            return str(err), 404
        except PermissionError as err:
            app.logger.error('Error logging in: %s', str(err))
            span.record_exception(err)
            return str(err), 401
        except SQLAlchemyError as err:
            app.logger.error('Error logging in: %s', str(err))
            span.record_exception(err)
            return 'failed to retrieve user information', 500

    @atexit.register
    def _shutdown():
        """Executed when web app is terminated."""
        app.logger.info("Stopping userservice.")

    # Set up logger
    app.logger.handlers = logging.getLogger('gunicorn.error').handlers
    app.logger.setLevel(logging.getLogger('gunicorn.error').level)
    app.logger.info('Starting userservice.')

    app.config['VERSION'] = os.environ.get('VERSION')
    app.config['EXPIRY_SECONDS'] = int(os.environ.get('TOKEN_EXPIRY_SECONDS'))
    private_key_path = os.environ.get('PRIV_KEY_PATH')
    if private_key_path:
        app.config['PRIVATE_KEY'] = Path(private_key_path).read_text(encoding='ascii')
    public_key_path = os.environ.get('PUB_KEY_PATH')
    if os.environ.get('PUB_KEY_PATH'):
        app.config['PUBLIC_KEY'] = Path(public_key_path).read_text(encoding='ascii')

    # Configure database connection
    try:
        users_db = UserDb(os.environ.get("ACCOUNTS_DB_URI"), app.logger)
    except OperationalError:
        app.logger.critical("users_db database connection failed")
        sys.exit(1)

    # Set up tracing and export spans to Open Telemetry
    if tracing.config:
        tracing.config.instrument_app(app)

    # Setup health checks and management endpoints
    ManagementEndpoints(app)

    def db_check():
        try:
            engine = users_db.engine
            result = engine.execute('SELECT 1')
            return result.first()[0] == 1
        except SQLAlchemyError as err:
            app.logger.error(f'DB health check failed: {err}')
            return False

    app.config.update(
        Z_ENDPOINTS={
            'check_functions': {
                'readiness': {
                    'db': db_check
                }
            }
        }
    )

    return app


if __name__ == "__main__":
    if not tracing.config:
        tracing.config = OpenTelemetryConfiguration(APP_NAME)
        tracing.config.setup_exporter()

    # Create an instance of flask server when called directly
    USERSERVICE = create_app()
    USERSERVICE.run(port=os.getenv('FLASK_RUN_PORT', 5001))
