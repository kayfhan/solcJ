# solcJ
Solidity compiler binaries, packed into jar for use in Java based projects.

We use jar in GSC project, And then we use code snippet for compilation:

```
String contractSrc =
"pragma solidity ^0.4.25;\n" +
       "contract gsc {" +
       "        function name() returns (string) {return \"gsc\";}\n" +
       "}";
SolidityCompiler.Result res = SolidityCompiler.compile(
                contractSrc.getBytes(), true, ABI, BIN, INTERFACE, METADATA);
System.out.println("Out: '" + res.output + "'");
System.out.println("Err: '" + res.errors + "'");

CompilationResult result = CompilationResult.parse(res.output);
System.out.println("result: " + result.getContractName());

CompilationResult.ContractMetadata contractMetadata = result.getContract("gsc");
System.out.println("ABI:          " + contractMetadata.abi);
System.out.println("Bytecode:     " + contractMetadata.bin);
System.out.println("metadata:     " + contractMetadata.metadata);
System.out.println("getInterface: " + contractMetadata.getInterface());
```
