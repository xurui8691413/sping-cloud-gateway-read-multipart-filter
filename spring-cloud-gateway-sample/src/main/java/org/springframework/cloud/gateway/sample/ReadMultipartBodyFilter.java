

package org.springframework.cloud.gateway.sample;

import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collections;

@Component
public class ReadMultipartBodyFilter implements GlobalFilter, Ordered {
	private final static Logger logger = LoggerFactory.getLogger(ReadMultipartBodyFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		return exchange.getRequest().getBody().collectList().flatMap(dataBuffers -> {
			byte[] totalBytes = dataBuffers.stream().map(dataBuffer -> {
				try {
					byte[] bytes = IOUtils.toByteArray(dataBuffer.asInputStream());
					logger.info(new String(bytes));
					return bytes;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).reduce(this::addBytes).get();
			ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
				@Override
				public Flux<DataBuffer> getBody() {
					return Flux.just(buffer(totalBytes));
				}
			};
			Mono<MultiValueMap<String, Part>> multiValueMapMono = repackageMultipartData(decorator, codecConfigurer);
			return multiValueMapMono.flatMap(part->{
				System.out.println(((FormFieldPart) part.getFirst("param")).value());
				return chain.filter(exchange.mutate().request(decorator).build());
			});
		});

	}
	

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
	
}
