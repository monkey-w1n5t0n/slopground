;; AST Node Structures
(defstruct ast-node type children)
(defstruct var-def name value)
(defstruct binary-op op left right)
(defstruct number-node value)
(defstruct symbol-node name)

;; Parser
(defun parse-expression (expr)
  "Parses a single Lisp expression into an AST node."
  (cond
    ((numberp expr) (make-number-node :value expr))
    ((symbolp expr) (make-symbol-node :name expr))
    ((listp expr)
     (let ((op (first expr)))
       (cond
         ((eq op 'defvar)
          (if (and (= (length expr) 3) (symbolp (second expr)))
              (make-ast-node :type 'defvar
                             :children (list (make-var-def :name (second expr)
                                                           :value (parse-expression (third expr)))))
              (error "Invalid defvar syntax: ~s" expr)))
         ((member op '(+ - * /))
          (if (= (length expr) 3)
              (make-ast-node :type 'binary-op
                             :children (list (make-binary-op :op op
                                                             :left (parse-expression (second expr))
                                                             :right (parse-expression (third expr)))))
              (error "Invalid binary operation syntax: ~s. Expected (op arg1 arg2)" expr)))
         ;; TODO: Add parsing for function definitions (defun)
         ;; TODO: Add progn for multiple expressions
         (t (error "Unknown operation: ~s" op)))))
    (t (error "Invalid Lisp code form: ~s" expr))))

(defun parse (code-list)
  "Parses a list of Lisp expressions into a list of AST nodes."
  (mapcar #'parse-expression code-list))

;; Code Generator
(defvar *next-register* 0)
(defvar *variable-map* (make-hash-table)) ; To store variable locations
(defvar *next-memory-location* 0)

(defun reset-register-allocator ()
  (setf *next-register* 0))

(defun initialize-memory-map ()
  (clrhash *variable-map*)
  (setf *next-memory-location* 0))

(defun get-register ()
  (let ((reg (format nil "R~a" *next-register*)))
    (incf *next-register*)
    reg))

(defun get-var-location (var-name)
  (or (gethash var-name *variable-map*)
      (progn
        (setf (gethash var-name *variable-map*) (format nil "MEM~a" *next-memory-location*))
        (incf *next-memory-location*)
        (gethash var-name *variable-map*))))

(defun generate-assembly (ast)
  "Generates assembly code from an AST node."
  (reset-register-allocator) ; Reset only registers for each top-level AST
  (generate-node-assembly ast))

(defun generate-node-assembly (node)
  "Recursively generates assembly for a given AST node."
  (cond
    ((number-node-p node)
     (let ((reg (get-register)))
       (list (format nil "LOAD ~a ~a" (number-node-value node) reg))))
    ((symbol-node-p node)
     (let ((reg (get-register))
           (mem-loc (get-var-location (symbol-node-name node))))
       ;; Assuming symbol means loading a variable's value
       (list (format nil "LOAD ~a ~a ; Load variable ~a" mem-loc reg (symbol-node-name node)))))
    ((ast-node-p node)
     (case (ast-node-type node)
       ('defvar
        (let* ((var-def (first (ast-node-children node)))
               (var-name (var-def-name var-def))
               (var-loc (get-var-location var-name))
               (val-ast (var-def-value var-def)))
          (append (generate-node-assembly val-ast)
                  (list (format nil "STORE R~a ~a ; Store ~a"
                                 (1- *next-register*) ; Value is in the last used register
                                 var-loc var-name)))))
       ('binary-op
        (let* ((op-details (first (ast-node-children node)))
               (op (binary-op-op op-details))
               (left-ast (binary-op-left op-details))
               (right-ast (binary-op-right op-details))
               (left-asm (generate-node-assembly left-ast))
               (left-reg (format nil "R~a" (1- *next-register*))) ; Register used by left operand
               (right-asm (generate-node-assembly right-ast))
               (right-reg (format nil "R~a" (1- *next-register*))) ; Register used by right operand
               (result-reg left-reg)) ; Reuse left operand's register for the result
          (decf *next-register*) ; Free up the right operand's register
          (append left-asm
                  right-asm
                  (list (format nil "~a ~a ~a ~a"
                                 (case op
                                   (+ "ADD")
                                   (- "SUB")
                                   (* "MUL")
                                   (/ "DIV"))
                                 left-reg right-reg result-reg)))))
       (t (error "Unsupported AST node type for code generation: ~s" (ast-node-type node)))))
    (t (error "Invalid node for code generation: ~s" node))))


(defun main ()
  (let ((args sb-ext:*posix-argv*))
    (if (< (length args) 2)
        (format t "Usage: sbcl --script transpiler.lisp <input-file.toy_lisp>~%")
        (let ((filename (second args)))
          (initialize-memory-map) ; Initialize variable/memory map once
          (handler-case
              (with-open-file (stream filename)
                (let ((all-code '())
                      (all-assembly '()))
                  ;; Read all expressions from the file
                  (do ((expr (read stream nil :eof) (read stream nil :eof)))
                      ((eq expr :eof))
                    (push expr all-code))
                  (setf all-code (nreverse all-code)) ; Maintain original order

                  (if (null all-code)
                      (format t ";; No code found in file.~%")
                      (progn
                        (format t ";; Input Lisp code list:~%~s~%~%" all-code)
                        (let ((asts (parse all-code)))
                          (format t ";; ASTs:~%~s~%~%" asts)
                          (dolist (ast asts)
                            (setf all-assembly (append all-assembly (generate-assembly ast))))
                          (format t ";; Assembly Output:~%")
                          (dolist (instr all-assembly)
                            (format t "~a~%" instr)))))))
            (error (e)
              (format t "Error reading file ~a: ~a~%" filename e)))))))

;; Entry point
(main)
