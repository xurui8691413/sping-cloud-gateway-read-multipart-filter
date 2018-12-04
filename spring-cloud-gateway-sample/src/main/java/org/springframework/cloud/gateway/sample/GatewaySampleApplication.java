/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.gateway.sample;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancerAutoConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author Spencer Gibb
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = ReactiveLoadBalancerAutoConfiguration.class)
@RestController
@RequestMapping("/test")
public class GatewaySampleApplication {

	@Value("server.port")
	private String port;

	@Bean
	public GlobalFilter filter(){
		return new ReadMultipartBodyFilter();
	}

/*	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {

		return builder.routes()

				.route(r -> r.order(-1)
						.path("/get")
						.filters(f -> f.prefixPath("/httpbin")
								.filter(new ThrottleGatewayFilter()
										.setCapacity(1)
										.setRefillTokens(1)
										.setRefillPeriod(10)
										.setRefillUnit(TimeUnit.SECONDS)))
						.uri(uri)
				)
				.build();
		//@formatter:on
	}*/


	/**
	 * @param formFieldPart
	 * @param fileFlux
	 * @param request
	 * @return
	 */
	@PostMapping(value = "/test/multipart")
	public Mono<ResponseEntity> multipart(@RequestPart("param") FormFieldPart formFieldPart, @RequestPart(value = "file", required = false) Flux<Part> fileFlux , ServerHttpRequest request) {
			return fileFlux
					.flatMap(part -> part.content().collectList().map(dataBuffers -> new DefaultDataBufferFactory().join(dataBuffers)))
					.collectList().map(dataBuffers -> new DefaultDataBufferFactory().join(dataBuffers))
					.map(dataBuffer -> {
//						CharBuffer charBuffer = StandardCharsets.UTF_8.decode(dataBuffer.asInputStream());
						String result = new BufferedReader(new InputStreamReader(dataBuffer.asInputStream()))
								.lines().collect(Collectors.joining());
//						DataBufferUtils.release(dataBuffer);
						System.out.println(result);
//						System.out.println(charBuffer.toString());
						return result;
					}).cast(String.class)
					.flatMap(s -> Mono.just(ResponseEntity.ok(s)));

	}

	@PostMapping(value = "/test/json", consumes = {"application/json"})
	public Mono<ResponseEntity> jsonTest(@RequestBody JSONObject json, ServerHttpRequest request) {
		System.out.println(json);
		return Mono.just(ResponseEntity.ok(json));
	}

	/**
	 */
	@GetMapping(value = "/test/controller")
	public Mono<ResponseEntity> asrBA() {
		return  Mono.just(ResponseEntity.ok("success"));

	}

	public static void main(String[] args) {
		SpringApplication.run(GatewaySampleApplication.class, args);
	}
}
