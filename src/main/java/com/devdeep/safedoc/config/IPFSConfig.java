package com.devdeep.safedoc.config;



import io.ipfs.api.IPFS;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IPFSConfig {

    @Bean
    public IPFS ipfs() {
        // Connect to local IPFS node. You can replace it with remote if needed.
        return new IPFS("/ip4/127.0.0.1/tcp/5001");
    }
}

