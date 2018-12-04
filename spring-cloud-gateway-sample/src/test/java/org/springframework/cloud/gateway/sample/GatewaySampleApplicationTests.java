/*
 * Copyright 2013-2018 the original author or authors.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;

import com.sun.org.apache.bcel.internal.generic.NEW;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.util.SocketUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { GatewaySampleApplicationTests.TestConfig.class},
		webEnvironment = DEFINED_PORT, properties = "management.server.port=${server.port}")
public class GatewaySampleApplicationTests {


	protected static int managementPort;

	protected WebTestClient webClient;
	protected String baseUri;

/*	@BeforeClass
	public static void beforeClass() {
		managementPort = SocketUtils.findAvailableTcpPort();

		System.setProperty("test.port", String.valueOf(managementPort));
	}

	@AfterClass
	public static void afterClass() {
		System.clearProperty("test.port");
	}*/

	@Before
	public void setup() {
		baseUri = "http://localhost:9090";
		this.webClient = WebTestClient.bindToServer().responseTimeout(Duration.ofSeconds(10)).baseUrl(baseUri).build();
	}

	@Test
	public void testMultipartFilter() throws IOException {


		webClient.post()
				.uri("/gateway/test/multipart")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.syncBody(generateBody())
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	public void testJsonFilter() {
		webClient.post()
				.uri("/gateway/test/json")
				.header("Content-Type",MediaType.APPLICATION_JSON_UTF8_VALUE)
				.syncBody("{\"test\":\"hello\"}")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	public void contextLoads() {
		webClient.get()
				.uri("/gateway/test/controller")
				.exchange()
				.expectStatus().isOk();
	}



	@Configuration
	@EnableAutoConfiguration
	@Import(GatewaySampleApplication.class)
	protected static class TestConfig {
	}

	private MultiValueMap<String, HttpEntity<?>> generateBody() throws IOException {
		DataBuffer buffer = buffer(IOUtils.toByteArray(new FileInputStream(new File(getClass().getClassLoader().getResource("TestFile.txt").getFile()))));

		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("param", "test");
		builder.asyncPart("file", Flux.just(buffer),DataBuffer.class) .headers(h -> {
			h.setContentDispositionFormData("file", "TestFile.txt");
			h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		});;
//		builder.part("file", new ClassPathResource("TestFile.txt", this.getClass()));
		return builder.build();
	}

	protected DataBuffer buffer(byte[] bytes) {
		NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
		DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}
}
