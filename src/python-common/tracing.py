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
import importlib.util as importer
import os

import flask
from flask_management_endpoints import Info

from grpc import Compression

from opentelemetry import trace

from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.resources import Resource

config = None


class OpenTelemetryConfiguration:
    tracing_enabled: bool = os.getenv('ENABLE_TRACING', False) and os.getenv('OTEL_EXPORTER_OTLP_ENDPOINT')
    tracer_provider_configured: bool = False

    def __init__(self, app_name: str):
        self.app_name = app_name

    def setup_tracer_provider(self):
        if self.tracing_enabled:
            resource = Resource.create(attributes=Info.trace_attributes(app_name=self.app_name))
            trace_provider = TracerProvider(resource=resource)
            trace.set_tracer_provider(trace_provider)
            self.tracer_provider_configured = True

    def setup_exporter(self):
        if self.tracing_enabled:
            if not self.tracer_provider_configured:
                self.setup_tracer_provider()

            otlp_exporter = OTLPSpanExporter(endpoint=os.getenv('OTEL_EXPORTER_OTLP_ENDPOINT'),
                                             compression=Compression.Gzip)
            span_processor = BatchSpanProcessor(otlp_exporter)
            trace.get_tracer_provider().add_span_processor(span_processor)

    def setup_logging(self):
        if self.tracing_enabled:
            if not self.tracer_provider_configured:
                self.setup_tracer_provider()
            if importer.find_spec('opentelemetry.instrumentation.logging'):
                from opentelemetry.instrumentation.logging import LoggingInstrumentor
                logging_instrumentor = LoggingInstrumentor()
                if not logging_instrumentor.is_instrumented_by_opentelemetry:
                    logging_instrumentor.instrument()

    def instrument_app(self, app: flask.Flask):
        """
        Sets up Open Telemetry integration and ensures that it is only set up
        a single time.
        :param app: reference to Flask application
        :return: True if tracing has been set up
        """
        if self.tracing_enabled:
            if not self.tracer_provider_configured:
                self.setup_tracer_provider()
            # Configure trace id integration with logging in case we are
            # executing Flask directly without gunicorn
            self.setup_logging()

            app.logger.info('✅ Tracing enabled')
            if importer.find_spec('opentelemetry.instrumentation.flask'):
                from opentelemetry.instrumentation.flask import FlaskInstrumentor
                flask_instrumentor = FlaskInstrumentor()
                if not flask_instrumentor.is_instrumented_by_opentelemetry:
                    flask_instrumentor.instrument_app(app)
                    app.logger.debug('Enabled Flask instrumentation')
            if importer.find_spec('opentelemetry.instrumentation.requests'):
                from opentelemetry.instrumentation.requests import RequestsInstrumentor
                request_instrumentor = RequestsInstrumentor()
                if not request_instrumentor.is_instrumented_by_opentelemetry:
                    request_instrumentor.instrument()
                    app.logger.debug('Enabled Requests instrumentation')
            if importer.find_spec('opentelemetry.instrumentation.jinja2'):
                from opentelemetry.instrumentation.jinja2 import Jinja2Instrumentor
                jinja2_instrumentor = Jinja2Instrumentor()
                if not jinja2_instrumentor.is_instrumented_by_opentelemetry:
                    jinja2_instrumentor.instrument()
                    app.logger.debug('Enabled Jinja instrumentation')
        else:
            app.logger.info('🚫 Tracing disabled')

