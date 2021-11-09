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

"""Web service for frontend
"""

import datetime
import json
import logging
import os

from decimal import Decimal
from pathlib import Path

import requests
from grpc import Compression
from requests.exceptions import HTTPError, RequestException
import jwt
from flask import Flask, abort, jsonify, make_response, redirect, \
    render_template, request, url_for

from opentelemetry import trace
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from opentelemetry.instrumentation.jinja2 import Jinja2Instrumentor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import Resource

from flask_management_endpoints import Info, ManagementEndpoints


# pylint: disable-msg=too-many-locals
def create_app():
    """Flask application factory to create instances
    of the Frontend Flask App
    """
    app = Flask('frontend')

    # Disabling unused-variable for lines with route decorated functions
    # as pylint thinks they are unused
    # pylint: disable=unused-variable
    @app.route("/")
    def root():
        """
        Renders home page or login page, depending on authentication status.
        """
        token = request.cookies.get(app.config['TOKEN_NAME'])
        if not verify_token(token):
            return login_page()
        return home()

    @app.route("/home")
    def home():
        """
        Renders home page. Redirects to /login if token is not valid
        """
        span = trace.get_current_span()
        token = request.cookies.get(app.config['TOKEN_NAME'])
        if not verify_token(token):
            # user isn't authenticated
            app.logger.debug('User isn\'t authenticated - redirecting to login page')
            span.add_event("user not authenticated")
            return redirect(url_for('login_page',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))
        token_data = jwt.decode(token, verify=False)
        display_name = token_data['name']
        username = token_data['user']
        account_id = token_data['acct']
        span.set_attributes({'account_id': account_id})

        # get balance
        app.logger.debug('Getting account balance')
        balance = read_json_response_from_remote(
            url=f'{app.config["BALANCES_URI"]}/{account_id}',
            service='balance-reader',
            token=token)

        # get history
        app.logger.debug('Getting transaction history')
        transaction_list = read_json_response_from_remote(
            url=f'{app.config["HISTORY_URI"]}/{account_id}',
            service='transaction-history',
            token=token)

        # get contacts
        app.logger.debug('Getting contacts')
        contacts = read_json_response_from_remote(
            url=f'{app.config["CONTACTS_URI"]}/{username}',
            service='contacts',
            token=token)

        _populate_contact_labels(account_id, transaction_list, contacts)

        return render_template('index.html',
                               cluster_name=cluster_name,
                               pod_name=pod_name,
                               pod_zone=pod_zone,
                               cymbal_logo=os.getenv('CYMBAL_LOGO', 'false'),
                               history=transaction_list,
                               balance=balance,
                               name=display_name,
                               account_id=account_id,
                               contacts=contacts,
                               message=request.args.get('msg', None),
                               bank_name=os.getenv('BANK_NAME', 'Bank of Sirius'))

    def read_json_response_from_remote(url: str, service: str, token: str):
        """
        Reads and returns the JSON output returned from a remote service URL.
        """
        with tracer.start_as_current_span(
                name=f'read_json_response_from_remote[{service}]',
                attributes={'service': service}) as span:
            hed = {'Authorization': 'Bearer ' + token}
            timeout = app.config['BACKEND_TIMEOUT']
            response_value = None

            try:
                response = requests.get(url=url, headers=hed, timeout=timeout)
            except IOError as err:
                span.record_exception(err, attributes={'timeout': timeout})
                app.logger.error('Error reading data from [%s]: %s', str(err))
                return None

            if response:
                with tracer.start_as_current_span('json_deserialize') as json_span:
                    try:
                        response_value = response.json()
                    except ValueError as err:
                        json_span.record_exception(err)
                        app.logger.error('Unable to parse JSON data from [%s]: %s', str(err))
            else:
                span.add_event('invalid HTTP response', {'error': True})

            return response_value

    def _populate_contact_labels(account_id, transactions, contacts):
        """
        Populate contact labels for the passed transactions.

        Side effect:
            Take each transaction and set the 'accountLabel' field with the label of
            the contact each transaction was associated with. If there was no
            associated contact, set 'accountLabel' to None.
            If any parameter is None, nothing happens.

        Params: account_id - the account id for the user owning the transaction list
                transactions - a list of transactions as key/value dicts
                            [{transaction1}, {transaction2}, ...]
                contacts - a list of contacts as key/value dicts
                        [{contact1}, {contact2}, ...]
        """
        with tracer.start_as_current_span(name='populate_contact_labels'):
            app.logger.debug('Populating contact labels')
            if account_id is None or transactions is None or contacts is None:
                return

            # Map contact accounts to their labels. If no label found, default to None.
            contact_map = {c['account_num']: c.get('label') for c in contacts}

            # Populate the 'accountLabel' field. If no match found, default to None.
            for trans in transactions:
                if trans['toAccountNum'] == account_id:
                    trans['accountLabel'] = contact_map.get(trans['fromAccountNum'])
                elif trans['fromAccountNum'] == account_id:
                    trans['accountLabel'] = contact_map.get(trans['toAccountNum'])

    @app.route('/payment', methods=['POST'])
    def payment():
        """
        Submits payment request to ledgerwriter service

        Fails if:
        - token is not valid
        - basic validation checks fail
        - response code from ledgerwriter is not 201
        """
        span = trace.get_current_span()
        token = request.cookies.get(app.config['TOKEN_NAME'])
        if not verify_token(token):
            # user isn't authenticated
            app.logger.error('Error submitting payment: user is not authenticated')
            span.add_event("user not authenticated")
            return abort(401)
        try:
            account_id = jwt.decode(token, verify=False)['acct']
            recipient = request.form['account_num']
            if recipient == 'add':
                recipient = request.form['contact_account_num']
                label = request.form.get('contact_label', None)
                if label:
                    # new contact. Add to contacts list
                    _add_contact(label,
                                 recipient,
                                 app.config['LOCAL_ROUTING'],
                                 False)

            transaction_data = {"fromAccountNum": account_id,
                                "fromRoutingNum": app.config['LOCAL_ROUTING'],
                                "toAccountNum": recipient,
                                "toRoutingNum": app.config['LOCAL_ROUTING'],
                                "amount": int(Decimal(request.form['amount']) * 100),
                                "uuid": request.form['uuid']}
            _submit_transaction(transaction_data)
            app.logger.info('Payment initiated successfully')
            return redirect(url_for('home',
                                    msg='Payment successful',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))

        except requests.exceptions.RequestException as err:
            span.record_exception(err)
            app.logger.error('Error submitting payment: %s', str(err))
        except UserWarning as warn:
            app.logger.error('Error submitting payment: %s', str(warn))
            span.add_event(str(warn))
            msg = f'Payment failed: {warn}'
            return redirect(url_for('home',
                                    msg=msg,
                                    _external=True,
                                    _scheme=app.config['SCHEME']))

        return redirect(url_for('home',
                                msg='Payment failed',
                                _external=True,
                                _scheme=app.config['SCHEME']))

    @app.route('/deposit', methods=['POST'])
    def deposit():
        """
        Submits deposit request to ledgerwriter service

        Fails if:
        - token is not valid
        - routing number == local routing number
        - response code from ledgerwriter is not 201
        """
        span = trace.get_current_span()
        token = request.cookies.get(app.config['TOKEN_NAME'])
        if not verify_token(token):
            # user isn't authenticated
            app.logger.error('Error submitting deposit: user is not authenticated.')
            span.add_event("user not authenticated")
            return abort(401)
        try:
            # get account id from token
            account_id = jwt.decode(token, verify=False)['acct']
            if request.form['account'] == 'add':
                external_account_num = request.form['external_account_num']
                external_routing_num = request.form['external_routing_num']
                if external_routing_num == app.config['LOCAL_ROUTING']:
                    raise UserWarning("invalid routing number")
                external_label = request.form.get('external_label', None)
                if external_label:
                    # new contact. Add to contacts list
                    _add_contact(external_label,
                                 external_account_num,
                                 external_routing_num,
                                 True)
            else:
                account_details = json.loads(request.form['account'])
                external_account_num = account_details['account_num']
                external_routing_num = account_details['routing_num']

            transaction_data = {"fromAccountNum": external_account_num,
                                "fromRoutingNum": external_routing_num,
                                "toAccountNum": account_id,
                                "toRoutingNum": app.config['LOCAL_ROUTING'],
                                "amount": int(Decimal(request.form['amount']) * 100),
                                "uuid": request.form['uuid']}
            _submit_transaction(transaction_data)
            app.logger.info('Deposit submitted successfully')
            return redirect(url_for('home',
                                    msg='Deposit successful',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))

        except requests.exceptions.RequestException as err:
            span.record_exception(err)
            app.logger.error('Error submitting deposit: %s', str(err))
        except UserWarning as warn:
            span.add_event(str(warn))
            app.logger.error('Error submitting deposit: %s', str(warn))
            msg = f'Deposit failed: {warn}'
            return redirect(url_for('home',
                                    msg=msg,
                                    _external=True,
                                    _scheme=app.config['SCHEME']))

        return redirect(url_for('home',
                                msg='Deposit failed',
                                _external=True,
                                _scheme=app.config['SCHEME']))

    def _submit_transaction(transaction_data):
        with tracer.start_as_current_span(name='submit_transaction'):
            app.logger.debug('Submitting transaction')
            token = request.cookies.get(app.config['TOKEN_NAME'])
            hed = {'Authorization': 'Bearer ' + token,
                   'content-type': 'application/json'}
            resp = requests.post(url=app.config["TRANSACTIONS_URI"],
                                 data=jsonify(transaction_data).data,
                                 headers=hed,
                                 timeout=app.config['BACKEND_TIMEOUT'])
            try:
                resp.raise_for_status() # Raise on HTTP Status code 4XX or 5XX
            except requests.exceptions.HTTPError as http_request_err:
                raise UserWarning(resp.text) from http_request_err

    def _add_contact(label, acct_num, routing_num, is_external_acct=False):
        """
        Submits a new contact to the contact service.

        Raise: UserWarning  if the response status is 4xx or 5xx.
        """
        with tracer.start_as_current_span(name='add_contact'):
            app.logger.debug('Adding new contact')
            token = request.cookies.get(app.config['TOKEN_NAME'])
            hed = {'Authorization': 'Bearer ' + token,
                   'content-type': 'application/json'}
            contact_data = {
                'label': label,
                'account_num': acct_num,
                'routing_num': routing_num,
                'is_external': is_external_acct
            }
            token_data = jwt.decode(token, verify=False)
            url = f"{app.config['CONTACTS_URI']}/{token_data['user']}"
            resp = requests.post(url=url,
                                 data=jsonify(contact_data).data,
                                 headers=hed,
                                 timeout=app.config['BACKEND_TIMEOUT'])
            try:
                resp.raise_for_status() # Raise on HTTP Status code 4XX or 5XX
            except requests.exceptions.HTTPError as http_request_err:
                raise UserWarning(resp.text) from http_request_err

    @app.route("/login", methods=['GET'])
    def login_page():
        """
        Renders login page. Redirects to /home if user already has a valid token
        """
        span = trace.get_current_span()
        token = request.cookies.get(app.config['TOKEN_NAME'])
        if verify_token(token):
            # already authenticated
            app.logger.debug('User already authenticated. Redirecting to /home')
            span.add_event('user already authenticated - redirecting')
            return redirect(url_for('home',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))

        return render_template('login.html',
                               cymbal_logo=os.getenv('CYMBAL_LOGO', 'false'),
                               cluster_name=cluster_name,
                               pod_name=pod_name,
                               pod_zone=pod_zone,
                               message=request.args.get('msg', None),
                               default_user=os.getenv('DEFAULT_USERNAME', ''),
                               default_password=os.getenv('DEFAULT_PASSWORD', ''),
                               bank_name=os.getenv('BANK_NAME', 'Bank of Sirius'))

    @app.route('/login', methods=['POST'])
    def login():
        """
        Submits login request to userservice and saves resulting token

        Fails if userservice does not accept input username and password
        """
        return _login_helper(request.form['username'],
                             request.form['password'])

    def _login_helper(username, password):
        with tracer.start_as_current_span(name='login_helper',
                                          attributes={'username': username}) as span:
            try:
                app.logger.debug('Logging in')
                req = requests.get(url=app.config["LOGIN_URI"],
                                   params={'username': username, 'password': password})
                req.raise_for_status() # Raise on HTTP Status code 4XX or 5XX

                # login success
                token = req.json()['token'].encode('utf-8')
                claims = jwt.decode(token, verify=False)
                max_age = claims['exp'] - claims['iat']
                resp = make_response(redirect(url_for('home',
                                                      _external=True,
                                                      _scheme=app.config['SCHEME'])))
                resp.set_cookie(app.config['TOKEN_NAME'], token, max_age=max_age)
                app.logger.info('Successfully logged in')
                return resp
            except (RequestException, HTTPError) as err:
                span.record_exception(err)
                app.logger.error('Error logging in: %s', str(err))
            return redirect(url_for('login',
                                    msg='Login Failed',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))

    @app.route("/signup", methods=['GET'])
    def signup_page():
        """
        Renders signup page. Redirects to /login if token is not valid
        """
        span = trace.get_current_span()
        token = request.cookies.get(app.config['TOKEN_NAME'])
        if verify_token(token):
            # already authenticated
            app.logger.debug('User already authenticated. Redirecting to /home')
            span.add_event('user already authenticated - redirecting')
            return redirect(url_for('home',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))
        return render_template('signup.html',
                               cymbal_logo=os.getenv('CYMBAL_LOGO', 'false'),
                               cluster_name=cluster_name,
                               pod_name=pod_name,
                               pod_zone=pod_zone,
                               bank_name=os.getenv('BANK_NAME', 'Bank of Sirius'))

    @app.route("/signup", methods=['POST'])
    def signup():
        """
        Submits signup request to userservice

        Fails if userservice does not accept input form data
        """
        span = trace.get_current_span()
        try:
            # create user
            app.logger.debug('Creating new user')
            resp = requests.post(url=app.config["USERSERVICE_URI"],
                                 data=request.form,
                                 timeout=app.config['BACKEND_TIMEOUT'])
            if resp.status_code == 201:
                # user created. Attempt login
                app.logger.info('New user created')
                span.add_event('new user created', {'username': request.form['username']})
                return _login_helper(request.form['username'],
                                     request.form['password'])
        except requests.exceptions.RequestException as err:
            span.record_exception(err)
            app.logger.error('Error creating new user: %s', str(err))
        return redirect(url_for('login',
                                msg='Error: Account creation failed',
                                _external=True,
                                _scheme=app.config['SCHEME']))

    @app.route('/logout', methods=['POST'])
    def logout():
        """
        Logs out user by deleting token cookie and redirecting to login page
        """
        span = trace.get_current_span()
        app.logger.info('Logging out')
        redirect_url = url_for('login_page', _external=True, _scheme=app.config['SCHEME'])
        span.set_attribute('redirect_url', redirect_url)

        resp = make_response(redirect(redirect_url))
        token_name = app.config['TOKEN_NAME']
        resp.delete_cookie(token_name)
        span.add_event('deleting token (cookie)', {'cookie_name': token_name})
        return resp

    def verify_token(token):
        """
        Validates token using userservice public key
        """
        with tracer.start_as_current_span(name='verify_token') as span:
            app.logger.debug('Verifying token')
            if token is None:
                span.add_event('token does not exist')
                return False
            try:
                jwt.decode(token, key=app.config['PUBLIC_KEY'], algorithms='RS256', verify=True)
                app.logger.debug('Token verified')
                return True
            except jwt.exceptions.InvalidTokenError as err:
                span.record_exception(err)
                app.logger.error('Error validating token: %s', str(err))
                return False

    # register html template formatters
    def format_timestamp_day(timestamp):
        """ Format the input timestamp day in a human readable way """
        # TODO: time zones?
        date = datetime.datetime.strptime(timestamp, app.config['TIMESTAMP_FORMAT'])
        return date.strftime('%d')

    def format_timestamp_month(timestamp):
        """ Format the input timestamp month in a human readable way """
        # TODO: time zones?
        date = datetime.datetime.strptime(timestamp, app.config['TIMESTAMP_FORMAT'])
        return date.strftime('%b')

    def format_currency(int_amount):
        """ Format the input currency in a human readable way """
        if int_amount is None:
            return '$---'
        decimal = abs(Decimal(int_amount)/100)
        amount_str = f'${decimal:0,.2f}'
        if int_amount < 0:
            amount_str = '-' + amount_str
        return amount_str

    transactions_api = os.environ.get('TRANSACTIONS_API_ADDR')
    user_service_api = os.environ.get('USERSERVICE_API_ADDR')
    balances_api = os.environ.get('BALANCES_API_ADDR')
    history_api = os.environ.get('HISTORY_API_ADDR')
    contacts_api = os.environ.get('CONTACTS_API_ADDR')
    scheme = os.environ.get('PREFERRED_URL_SCHEME', os.environ.get('SCHEME', 'http'))

    # set up global variables
    app.config['SCHEME'] = scheme
    app.config["TRANSACTIONS_URI"] = f"{scheme}://{transactions_api}/transactions"
    app.config["USERSERVICE_URI"] = f"{scheme}://{user_service_api}/users"
    app.config["BALANCES_URI"] = f"{scheme}://{balances_api}/balances"
    app.config["HISTORY_URI"] = f"{scheme}://{history_api}/transactions"
    app.config["LOGIN_URI"] = f"{scheme}://{user_service_api}/login"
    app.config["CONTACTS_URI"] = f"{scheme}://{contacts_api}/contacts"
    app.config['LOCAL_ROUTING'] = os.getenv('LOCAL_ROUTING_NUM')
    app.config['BACKEND_TIMEOUT'] = 4  # timeout in seconds for calls to the backend
    app.config['TOKEN_NAME'] = 'token'
    app.config['TIMESTAMP_FORMAT'] = '%Y-%m-%dT%H:%M:%S.%f%z'

    public_key_path = os.environ.get("PUB_KEY_PATH")
    if public_key_path:
        app.config["PUBLIC_KEY"] = Path(public_key_path).read_text(encoding='ascii')

    k8s_info = Info.k8s()
    # We do not have a good portable way to get the cluster name
    cluster_name = "unknown"
    if k8s_info and 'k8s.pod.name' in k8s_info:
        pod_name = k8s_info['k8s.pod.name']
    else:
        pod_name = "unknown"
    # get GKE node zone
    pod_zone = "unknown"

    # register formater functions
    app.jinja_env.globals.update(format_currency=format_currency)
    app.jinja_env.globals.update(format_timestamp_month=format_timestamp_month)
    app.jinja_env.globals.update(format_timestamp_day=format_timestamp_day)

    # Set up logging
    app.logger.handlers = logging.getLogger('gunicorn.error').handlers
    app.logger.setLevel(logging.getLogger('gunicorn.error').level)
    app.logger.info('Starting frontend service')

    # Set up tracing and export spans to Open Telemetry
    if os.environ['ENABLE_TRACING'] == "true" and os.getenv('OTEL_EXPORTER_OTLP_ENDPOINT'):
        app.logger.info("✅ Tracing enabled")
        resource = Resource.create(attributes=Info.trace_attributes(app_name=app.name))
        trace_provider = TracerProvider(resource=resource)
        trace.set_tracer_provider(trace_provider)
        otlp_exporter = OTLPSpanExporter(endpoint=os.getenv('OTEL_EXPORTER_OTLP_ENDPOINT'),
                                         compression=Compression.Gzip)
        span_processor = BatchSpanProcessor(otlp_exporter)
        trace.get_tracer_provider().add_span_processor(span_processor)
        flask_instrumentor = FlaskInstrumentor()
        if not flask_instrumentor.is_instrumented_by_opentelemetry:
            flask_instrumentor.instrument_app(app, tracer_provider=trace_provider)
        request_instrumentor = RequestsInstrumentor()
        if not request_instrumentor.is_instrumented_by_opentelemetry:
            request_instrumentor.instrument()
        jinja2_instrumentor = Jinja2Instrumentor()
        if not jinja2_instrumentor.is_instrumented_by_opentelemetry:
            jinja2_instrumentor.instrument()
    else:
        app.logger.info("🚫 Tracing disabled")

    tracer = trace.get_tracer(app.name)

    # Setup health checks and management endpoints
    ManagementEndpoints(app)
    app.config.update(
        Z_ENDPOINTS={
            "service_dependencies": {
                'balance_api': os.environ.get('BALANCES_API_ADDR'),
                'transactions_api': os.environ.get('TRANSACTIONS_API_ADDR'),
                'history_api': os.environ.get('HISTORY_API_ADDR'),
                'user_service_api': os.environ.get('USERSERVICE_API_ADDR'),
                'contacts_api': os.environ.get('CONTACTS_API_ADDR')
            }
        }
    )

    return app


if __name__ == "__main__":
    # Create an instance of flask server when called directly
    FRONTEND = create_app()
    FRONTEND.run()
