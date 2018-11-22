package org.gsc.solc;

import org.gsc.solidity.compiler.CompilationResult;
import org.gsc.solidity.compiler.SolidityCompiler;
import org.junit.Test;

import java.io.IOException;

import static org.gsc.solidity.compiler.SolidityCompiler.Options.*;

public class SolcTest {


    @Test
    public void solcTest() throws IOException {
        String contractSrc =
               "pragma solidity ^0.4.25;\n" +
                        "contract cont {" +
                        "        function name() returns (string) {return \"cont\";}\n" +
                        "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
        System.out.println("Out: '" + res.output + "'");
        System.out.println("Err: '" + res.errors + "'");

        System.out.println("-------------------------------------------------------------------------------------------");

        CompilationResult result = CompilationResult.parse(res.output);
        System.out.println("result: " + result.getContractName());

        CompilationResult.ContractMetadata contractMetadata = result.getContract("cont");
        System.out.println("ABI:          " + contractMetadata.abi);
        System.out.println("Bytecode:     " + contractMetadata.bin);
        System.out.println("metadata:     " + contractMetadata.metadata);
        System.out.println("getInterface: " + contractMetadata.getInterface());

        System.out.println("-------------------------------------------------------------------------------------------");

    }
}

