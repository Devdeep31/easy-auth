package com.devdeep.safedoc.config;

import com.safedoc.contracts.CertificateRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

@Configuration
public class BlockchainConfig {

    @Value("${blockchain.network.url}")
    private String networkUrl;

    @Value("${blockchain.contract.address}")
    private String contractAddress;

    @Value("${blockchain.owner.privateKey}")
    private String ownerPrivateKey;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(networkUrl));
    }

    @Bean
    public Credentials credentials() {
        return Credentials.create(ownerPrivateKey);
    }

    @Bean
    public CertificateRegistry contract(Web3j web3j, Credentials credentials) {
        return CertificateRegistry.load(
                contractAddress,
                web3j,
                credentials,
                new DefaultGasProvider()
        );
    }
}