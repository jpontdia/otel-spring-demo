package com.example.restservice;

import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();
	private final OpenTelemetry openTelemetryClient;

	public GreetingController(OpenTelemetry openTelemetryClient) {
		this.openTelemetryClient = openTelemetryClient;
	}

	/*
	 * Documentation: opentelemetry.io/docs/instrumentation/java/manual/#acquiring-a-tracer
     */
	@GetMapping("/greeting")
	public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {

		Span span = getTracer().spanBuilder("greeting").startSpan();
		log.info("Start spanId: {}", span.getSpanContext().getSpanId());

		//Add process properties
		span.setAttribute("userDefined1", "1");
		span.setAttribute("name", name);

		// Make the span the current span
		String formattedName = "";
		try (Scope ignored = span.makeCurrent()) {
			// In this scope, the span is the current/active span
			log.info("Call getFormattedGreeting()");
			Thread.sleep(200);
			formattedName= getFormattedGreeting(span, name);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			span.end();
			log.info("End spanId: {}", span.getSpanContext().getSpanId());
		}
		return new Greeting(counter.incrementAndGet(), formattedName);
	}

	String getFormattedGreeting(Span parentSpan, String name) {
		String formattedName="";
		Span childSpan = getTracer().spanBuilder("getFormattedGreeting")
				.setParent(Context.current().with(parentSpan))
				.startSpan();
		log.info("  Start spanId: {}", childSpan.getSpanContext().getSpanId());

		try {
			Thread.sleep(800);
			formattedName = String.format(template, name);
			childSpan.setAttribute("formattedName", formattedName);
	    }
        catch (Exception e) {
            e.printStackTrace();
		} finally {
			childSpan.end();
			log.info("  End spanId: {}", childSpan.getSpanContext().getSpanId());
		}
		return formattedName;
	}

	Tracer getTracer() {

		//Acquiring a Tracer
		return openTelemetryClient.getTracer(
				"instrumentation-library-name",
				"1.0.0");
	}
}
