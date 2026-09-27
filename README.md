# CalcPremium

A custom programming language implementation built in **Java** using **ANTLR4**, featuring lexical and syntactic analysis, AST construction, semantic analysis, intermediate-code generation, and a stack-based virtual machine component.

The project explores the core stages of programming-language implementation, from parsing source code to generating an instruction-based intermediate representation.

---

## Overview

CalcPremium was developed as an educational programming-language project focused on understanding how source code is processed and transformed through different stages of a compiler/interpreter pipeline.

The implementation includes:

- Custom ANTLR4 grammar
- Lexical analysis
- Syntax analysis
- Concrete Syntax Tree (CST) generation
- CST-to-AST transformation
- Abstract Syntax Tree (AST) representation
- Semantic analysis
- Intermediate-code generation
- Stack-based virtual machine implementation
- Interactive prompt and source-file input

The main processing pipeline transforms source code through multiple representations before producing a custom instruction sequence.

---

## Processing Pipeline

```text
Source Code
    |
    v
Lexer / Scanner
    |
    v
Parser
    |
    v
Concrete Syntax Tree
    |
    v
CST -> AST Conversion
    |
    v
Abstract Syntax Tree
    |
    v
Semantic Analysis
    |
    v
Intermediate-Code Generation
    |
    v
Instruction Representation
```

The command-line entry point currently performs the complete pipeline through intermediate-code generation and prints the generated instructions.

A separate stack-based virtual machine component is implemented for executing the custom instruction representation.

---

## Language Features

The CalcPremium grammar and AST support several common programming-language constructs.

### Data Types

The language includes support for:

- Integer values
- Boolean values
- Arrays
- Typed variables
- Function parameters
- Function return types

### Control Flow

Supported control-flow constructs include:

- `if` statements
- `else` branches
- `while` loops
- `for` loops
- `break`
- `continue`
- `return`

### Functions

Functions are represented through dedicated AST nodes and support:

- Function declarations
- Typed parameters
- Return types
- Function bodies
- Function calls

### Expressions

The language supports expression processing including:

- Literal values
- Variable references
- Assignment
- Arithmetic operations
- Relational operations
- Logical operations
- Array access
- Function calls

---

## Compiler Front-End

### Lexical Analysis

The scanner processes source input and produces the token stream consumed by the parser.

ANTLR4 is used as the underlying language-processing framework.

### Syntax Analysis

The parser validates the token stream according to the `CalcPremium.g4` grammar and generates a concrete syntax tree.

Syntax errors are reported with source position information.

### CST to AST Conversion

The generated concrete syntax tree is transformed into a custom Abstract Syntax Tree.

The AST contains dedicated representations for language constructs such as:

```text
FunctionDeclaration
IfStatement
WhileStatement
ForStatement
ReturnStatement
BreakStatement
ContinueStatement
DeclarationStatement
Expression
VariableReference
ArrayType
PrimitiveType
```

This creates a representation independent of the ANTLR parse tree and suitable for later compiler stages.

---

## Semantic Analysis

After AST construction, the program is passed through a dedicated semantic-analysis stage.

```text
AST
 |
 v
SemanticAnalyzer
 |
 v
Validated Program
```

Semantic validation takes place before intermediate code is generated.

If semantic errors are detected, compilation stops before the code-generation stage.

---

## Intermediate-Code Generation

After successful semantic analysis, the AST is compiled into a custom instruction representation.

The compiler is implemented in:

```text
rs.raf.gen.NotQuiteByteCodeCompiler
```

and produces `Instruction` objects representing lower-level program operations.

The code generator handles constructs including:

- Variable declarations
- Assignments
- Arithmetic expressions
- Logical expressions
- Conditional branches
- Loops
- `break` and `continue`
- Function calls
- Return statements
- Array operations

The generated instruction sequence is printed by the current command-line pipeline for inspection.

---

## Virtual Machine

The project also contains a stack-based virtual machine implemented in:

```text
rs.raf.vm.VM
```

The VM operates on the custom instruction representation and maintains:

- Operand stack
- Local variables
- Instruction pointer
- Runtime values

Implemented instruction behavior includes operations for:

- Constants
- Local-variable loading and storing
- Arithmetic
- Comparison
- Logical operations
- Conditional and unconditional jumps
- Arrays
- Printing
- Returning values

The VM is implemented as a separate execution component. The current `Main` pipeline generates and prints intermediate instructions rather than automatically passing them to the VM for end-to-end execution.

---

## Architecture

The main source structure is organized by compiler responsibility:

```text
src/main/
├── antlr4/
│   └── calcpremium/
│       └── parser/
│           └── CalcPremium.g4
│
└── java/
    └── rs/
        └── raf/
            ├── Main.java
            ├── ast/
            ├── calc/
            ├── gen/
            ├── lexer/
            ├── parser/
            ├── semantic/
            ├── utils/
            └── vm/
```

### Main Components

| Package | Responsibility |
|---|---|
| `ast` | Abstract Syntax Tree representation |
| `lexer` | Lexical analysis and tokenization |
| `parser` | Parsing and CST-to-AST conversion |
| `semantic` | Semantic analysis |
| `gen` | Intermediate-code generation |
| `vm` | Stack-based virtual machine |
| `calc` | Error state and language processing support |
| `utils` | Formatting and utility functionality |

---

## Interactive and File Input

The application supports two input modes.

### Interactive Prompt

Running the application without a source-file argument starts an interactive prompt:

```text
> 
```

Input is processed through the language pipeline until `exit` is entered.

### Source File

When a single command-line argument is supplied, it is interpreted as the path to a source file.

The file is then processed through the same compiler pipeline.

---

## Technologies

- **Java 21**
- **ANTLR4 4.13.1**
- **Maven**
- **Lombok**

---

## Building the Project

### Prerequisites

Install:

- JDK 21
- Apache Maven

Verify the installation:

```bash
java -version
mvn -version
```

### Clone

```bash
git clone https://github.com/MihailoDragicevic1/calc-premium.git
cd calc-premium
```

### Build

```bash
mvn clean package
```

or:

```bash
mvn clean test
```

The project currently does not contain an automated test suite, so `mvn clean test` primarily verifies grammar generation and successful compilation.

---

## Running the Project

After packaging:

```bash
java -jar target/calc-premium-1.0-SNAPSHOT.jar
```

This starts the interactive prompt.

To process a source file:

```bash
java -jar target/calc-premium-1.0-SNAPSHOT.jar path/to/program
```

The application processes the source code and outputs representations produced during the compilation pipeline, including the syntax tree, AST, semantic-analysis status, and generated intermediate instructions.

---

## Current Scope

CalcPremium is an educational programming-language implementation rather than a production compiler.

The current main application integrates:

```text
Lexing
  ->
Parsing
  ->
CST-to-AST Conversion
  ->
Semantic Analysis
  ->
Intermediate-Code Generation
```

A custom virtual machine is also implemented as a separate component for execution of the instruction representation, but it is not currently connected to the final stage of the `Main` command-line pipeline.

This distinction reflects the current implementation state of the project.

---

## Possible Improvements

Potential future work includes:

- Integrating the VM directly into the main execution pipeline
- Expanding semantic validation
- Improving runtime error reporting
- Adding automated unit and integration tests
- Adding example CalcPremium programs
- Improving command-line options
- Expanding the instruction set
- Improving source-location tracking across compiler stages

---

## Author

**Mihailo Dragićević**

Computer Science student at the Faculty of Computing (RAF), Union University in Belgrade.

[GitHub](https://github.com/MihailoDragicevic1) · [LinkedIn](https://www.linkedin.com/in/mihailo-dragicevic-3aaa81308/)