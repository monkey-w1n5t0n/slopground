# Toy Lisp to Toy Assembly Transpiler

This project is a simple transpiler that converts code written in a toy Lisp dialect into a toy Assembly language.

## Running the Transpiler

To transpile a `.toy_lisp` file, use SBCL (Steel Bank Common Lisp):

```bash
sbcl --script transpiler.lisp <input-file.toy_lisp>
```

For example:
```bash
sbcl --script transpiler.lisp example.toy_lisp
```

This will print the generated toy assembly code to standard output.

## Supported Toy Lisp Features (So Far)

*   **Variable Definition**: `(defvar variable-name value)`
    *   `variable-name`: A symbol representing the name of the variable.
    *   `value`: A number or a simple arithmetic expression.
*   **Arithmetic Operations**:
    *   `(+ expression1 expression2)`
    *   `(- expression1 expression2)`
    *   `(* expression1 expression2)`
    *   `(/ expression1 expression2)`
    *   Expressions can be numbers or other variables.

## Target Toy Assembly Language Instructions

*   `LOAD value register`: Load a numeric value or value from a memory location into a register.
*   `STORE register memory_location`: Store the value from a register into a memory location.
*   `ADD reg1 reg2 result_reg`: Add values from `reg1` and `reg2`, store in `result_reg`.
*   `SUB reg1 reg2 result_reg`: Subtract `reg2` from `reg1`, store in `result_reg`.
*   `MUL reg1 reg2 result_reg`: Multiply values from `reg1` and `reg2`, store in `result_reg`.
*   `DIV reg1 reg2 result_reg`: Divide `reg1` by `reg2`, store in `result_reg`.
*   `CALL function_name`: Call a function. (Future)
*   `RET`: Return from a function. (Future)
*   `DEFVAR var_name`: (Conceptual) Declares a variable in assembly, mapped to memory locations.
*   `DEFN func_name`: (Conceptual) Declares a function in assembly. (Future)

## Current Limitations & Future Work

*   The transpiler processes only the *first* S-expression from the input file. To process multiple forms, they should be wrapped in a `(progn ...)` block, and `progn` needs to be implemented.
*   No support for function definitions (`defun`) or function calls yet.
*   Error handling is basic.
*   Register allocation is very simple.
*   Variable scoping is global.