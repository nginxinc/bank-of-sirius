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
import os

import bunyan
import sys

import tracing

APP_NAME = os.getenv('APP_NAME')
if not APP_NAME:
    sys.exit('Missing required environment variable: APP_NAME')

tracing.config = tracing.OpenTelemetryConfiguration(app_name=APP_NAME)
tracing.config.setup_tracer_provider()

# Conditionally enable the Open Telemetry trace ids in the gunicorn logs
# only if Open Telemetry is enabled and logging is configured.
if tracing.config.tracing_enabled:
    tracing.config.setup_logging()

# formatter = bunyan.BunyanFormatter()

# By using APP_NAME we can share this gunicorn config file with all of
# the Python Flask services.
wsgi_app = f'{APP_NAME}:create_app()'

# By not using a typical logging.conf file to configure logging, we
# can dynamically set the logging format such that it will output
# trace ids when Open Telemetry is enabled.
logconfig_dict = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "bunyan": {
            '()': 'bunyan.BunyanFormatter'
        }
    },
    "handlers": {
        "console": {
            'class': 'logging.StreamHandler',
            'formatter': 'bunyan',
            'stream': 'ext://sys.stdout'
        }
    },
    "loggers": {
        "root": {
            'handlers': ['console']
        },
        'gunicorn.error': {
            'formatter': 'generic',
            'handlers': ['console'],
            'propagate': 0,
            'qualname': 'gunicorn.error'
        },
        'frontend': {
            'formatter': 'generic',
            'handlers': ['console'],
            'propagate': 0,
            'qualname': 'frontend'
        },
    }
}


def post_fork(server, worker):
    server.log.info("Worker spawned (pid: %s)", worker.pid)
    # The BatchSpanProcessor is not fork-safe and doesn’t work well
    # with application servers (Gunicorn, uWSGI) which are based on
    # the pre-fork web server model. The BatchSpanProcessor spawns
    # a thread to run in the background to export spans to the
    # telemetry backend. During the fork, the child process
    # inherits the lock which is held by the parent process and
    # deadlock occurs. We can use fork hooks to get around this
    # limitation of the span processor.
    # See: https://opentelemetry-python.readthedocs.io/en/latest/examples/fork-process-model/README.html

    # With the above in mind, we setup the Open Telemetry exporters here
    tracing.config.setup_exporter()
