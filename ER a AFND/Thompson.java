/*
    Thompson.java
    Compilador para Expresiones Regulares a Autómatas Finitos No Deterministas (AFND). Actualmente configurado para trabajar solo con expresiones regulares que utilizan el alfabeto ['a', 'z'].
    
    Este compilador funciona de izquierda a derecha, dando precedencia a los caracteres de la izquierda sobre los de la derecha. Esta es la forma más débil de precedencia en el compilador, después de la precedencia de operadores.
    
    Sintaxis de operadores:
        '|' para unión (menor precedencia)
        'ab' para concatenar algunos elementos a y b. Es decir, concatenación sin operador (precaución media)
        '*' para el cierre de Kleene (mayor precedencia)
        '(' y ')' para controlar la precedencia de operadores
*/
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Stack;

public class Thompson {
    /*
        Trans - objeto se utiliza como una tupla de 3 elementos para representar transiciones
        (estado desde, símbolo de la ruta de transición, estado a)
    */
    public static class Trans {
        public int state_from, state_to;
        public char trans_symbol;

        public Trans(int v1, int v2, char sym) {
            this.state_from = v1;
            this.state_to = v2;
            this.trans_symbol = sym;
        }
    }

    /*
        AFND - sirve como el gráfico que representa el Autómata Finito No Determinista. Usaremos esto para combinar mejor los estados.
    */
    public static class AFND {
        public ArrayList<Integer> states;
        public ArrayList<Trans> transitions;
        public int final_state;

        public AFND() {
            this.states = new ArrayList<Integer>();
            this.transitions = new ArrayList<Trans>();
            this.final_state = 0;
        }

        public AFND(int size) {
            this.states = new ArrayList<Integer>();
            this.transitions = new ArrayList<Trans>();
            this.final_state = 0;
            this.setStateSize(size);
        }

        public AFND(char c) {
            this.states = new ArrayList<Integer>();
            this.transitions = new ArrayList<Trans>();
            this.setStateSize(2);
            this.final_state = 1;
            this.transitions.add(new Trans(0, 1, c));
        }

        public void setStateSize(int size) {
            for (int i = 0; i < size; i++)
                this.states.add(i);
        }

        public void display() {
            for (Trans t : transitions) {
                System.out.println("(" + t.state_from + ", " + t.trans_symbol +
                        ", " + t.state_to + ")");
            }
        }
    }

    /*
        kleene() - Operador de expresión regular de mayor precedencia. Algoritmo de Thompson para el cierre de Kleene.
    */
    public static AFND kleene(AFND n) {
        AFND result = new AFND(n.states.size() + 2);
        result.transitions.add(new Trans(0, 1, 'E')); // nueva transición para q0

        // copiar transiciones existentes
        for (Trans t : n.transitions) {
            result.transitions.add(new Trans(t.state_from + 1,
                    t.state_to + 1, t.trans_symbol));
        }

        // agregar transición vacía desde el estado final de n al nuevo estado final.
        result.transitions.add(new Trans(n.states.size(),
                n.states.size() + 1, 'E'));

        // Regresar al último estado de n al estado inicial de n.
        result.transitions.add(new Trans(n.states.size(), 1, 'E'));

        // Agregar transición vacía desde el nuevo estado inicial al nuevo estado final.
        result.transitions.add(new Trans(0, n.states.size() + 1, 'E'));

        result.final_state = n.states.size() + 1;
        return result;
    }

    /*
        concat() - Algoritmo de Thompson para la concatenación. Precedencia media.
    */
    public static AFND concat(AFND n, AFND m) {
        m.states.remove(0); // eliminar el estado inicial de m

        // copiar las transiciones de AFND m a n y manejar la conexión entre n y m
        for (Trans t : m.transitions) {
            n.transitions.add(new Trans(t.state_from + n.states.size() - 1,
                    t.state_to + n.states.size() - 1, t.trans_symbol));
        }

        // llevar m y combinarlo con n después de borrar el estado inicial de m
        for (Integer s : m.states) {
            n.states.add(s + n.states.size() + 1);
        }

        n.final_state = n.states.size() + m.states.size() - 2;
        return n;
    }

    /*
        union() - Operador de expresión regular de menor precedencia. Algoritmo de Thompson para la unión (o). 
    */
    public static AFND union(AFND n, AFND m) {
        AFND result = new AFND(n.states.size() + m.states.size() + 2);

        // la ramificación de q0 al inicio de n
        result.transitions.add(new Trans(0, 1, 'E'));

        // copiar las transiciones existentes de n
        for (Trans t : n.transitions) {
            result.transitions.add(new Trans(t.state_from + 1,
                    t.state_to + 1, t.trans_symbol));
        }

        // transición desde el último estado de n al estado final
        result.transitions.add(new Trans(n.states.size(),
                n.states.size() + m.states.size() + 1, 'E'));

        // la ramificación de q0 al inicio de m
        result.transitions.add(new Trans(0, n.states.size() + 1, 'E'));

        // copiar las transiciones existentes de m
        for (Trans t : m.transitions) {
            result.transitions.add(new Trans(t.state_from + n.states.size()
                    + 1, t.state_to + n.states.size() + 1, t.trans_symbol));
        }

        // transición desde el último estado de m al estado final
        result.transitions.add(new Trans(m.states.size() + n.states.size(),
                n.states.size() + m.states.size() + 1, 'E'));

        // 2 nuevos estados y desplazamiento de m para evitar la repetición del último n y el primer m
        result.final_state = n.states.size() + m.states.size() + 1;
        return result;
    }

    // simplificar las comprobaciones repetidas de condiciones booleanas
    public static boolean alpha(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static boolean alphabet(char c) {
        return alpha(c) || c == 'E';
    }

    public static boolean regexOperator(char c) {
        return c == '(' || c == ')' || c == '*' || c == '|';
    }

    public static boolean validRegExChar(char c) {
        return alphabet(c) || regexOperator(c);
    }

    // validRegEx() - verifica si la cadena dada es una expresión regular válida.
    public static boolean validRegEx(String regex) {
        if (regex.isEmpty())
            return false;
        for (char c : regex.toCharArray())
            if (!validRegExChar(c))
                return false;
        return true;
    }

    /*
        compile() - compila la expresión regular dada en un AFND utilizando el Algoritmo de Construcción de Thompson. Implementará un modelo de pila de compilador típico para simplificar el procesamiento de la cadena. Esto da precedencia descendente a los caracteres a la derecha.
    */
    public static AFND compile(String regex) {
        if (!validRegEx(regex)) {
            System.out.println("Entrada de Expresión Regular Inválida.");
            return new AFND(); // AFND vacío si la expresión regular no es válida
        }

        Stack<Character> operators = new Stack<Character>();
        Stack<AFND> operands = new Stack<AFND>();
        Stack<AFND> concat_stack = new Stack<AFND>();
        boolean ccflag = false; // bandera de concatenación
        char op, c; // carácter actual de la cadena
        int para_count = 0;
        AFND AFND1, AFND2;

        for (int i = 0; i < regex.length(); i++) {
            c = regex.charAt(i);
            if (alphabet(c)) {
                operands.push(new AFND(c));
                if (ccflag) { // concatenar esto con el anterior
                    operators.push('.'); // '.' se utiliza para representar la concatenación.
                } else
                    ccflag = true;
            } else {
                if (c == ')') {
                    ccflag = false;
                    if (para_count == 0) {
                        System.out.println("Error: Más paréntesis de cierre que paréntesis de apertura.");
                        System.exit(1);
                    } else {
                        para_count--;
                    }
                    // procesar elementos en la pila hasta '('
                    while (!operators.empty() && operators.peek() != '(') {
                        op = operators.pop();
                        if (op == '.') {
                            AFND2 = operands.pop();
                            AFND1 = operands.pop();
                            operands.push(concat(AFND1, AFND2));
                        } else if (op == '|') {
                            AFND2 = operands.pop();

                            if (!operators.empty() &&
                                    operators.peek() == '.') {

                                concat_stack.push(operands.pop());
                                while (!operators.empty() &&
                                        operators.peek() == '.') {

                                    concat_stack.push(operands.pop());
                                    operators.pop();
                                }
                                AFND1 = concat(concat_stack.pop(),
                                        concat_stack.pop());
                                while (concat_stack.size() > 0) {
                                    AFND1 = concat(AFND1, concat_stack.pop());
                                }
                            } else {
                                AFND1 = operands.pop();
                            }
                            operands.push(union(AFND1, AFND2));
                        }
                    }
                } else if (c == '*') {
                    operands.push(kleene(operands.pop()));
                    ccflag = true;
                } else if (c == '(') { // si es cualquier otro operador: empujar
                    operators.push(c);
                    para_count++;
                } else if (c == '|') {
                    operators.push(c);
                    ccflag = false;
                }
            }
        }
        while (operators.size() > 0) {
            if (operands.empty()) {
                System.out.println("Error: Desbalance entre operandos y operadores.");
                System.exit(1);
            }
            op = operators.pop();
            if (op == '.') {
                AFND2 = operands.pop();
                AFND1 = operands.pop();
                operands.push(concat(AFND1, AFND2));
            } else if (op == '|') {
                AFND2 = operands.pop();
                if (!operators.empty() && operators.peek() == '.') {
                    concat_stack.push(operands.pop());
                    while (!operators.empty() && operators.peek() == '.') {
                        concat_stack.push(operands.pop());
                        operators.pop();
                    }
                    AFND1 = concat(concat_stack.pop(),
                            concat_stack.pop());
                    while (concat_stack.size() > 0) {
                        AFND1 = concat(AFND1, concat_stack.pop());
                    }
                } else {
                    AFND1 = operands.pop();
                }
                operands.push(union(AFND1, AFND2));
            }
        }
        return operands.pop();
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String line;
        System.out.println("\nIngrese una expresión regular con el " +
                "alfabeto ['a', 'z'] y E para el vacío " + "\n* para el cierre de Kleene" +
                "\nLos elementos sin nada entre ellos indican concatenación " +
                "\n| para unión \n\":q\" para salir");
        while (sc.hasNextLine()) {
            System.out.println("Ingrese una expresión regular con el " +
                    "alfabeto ['a', 'z'] y E para el vacío " + "\n* para el cierre de Kleene" +
                    "\nLos elementos sin nada entre ellos indican concatenación " +
                    "\n| para unión \n\":q\" para salir");
            line = sc.nextLine();
            if (line.equals(":q") || line.equals("QUIT"))
                break;
            AFND AFND_of_input = compile(line);
            System.out.println("\nAFND:");
            AFND_of_input.display();
        }
    }
}
