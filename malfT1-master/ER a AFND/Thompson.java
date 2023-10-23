
/*hola erii
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
import java.util.*;
import java.util.regex.*;
import java.util.List;
import java.util.stream.Collectors;

public class Thompson {
    /*
     * Trans - objeto se utiliza como una tupla de 3 elementos para representar
     * transiciones
     * (estado desde, símbolo de la ruta de transición, estado a)
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
     * AFND - sirve como el gráfico que representa el Autómata Finito No
     * Determinista. Usaremos esto para combinar mejor los estados.
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
     * kleene() - Operador de expresión regular de mayor precedencia. Algoritmo de
     * Thompson para el cierre de Kleene.
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
     * concat() - Algoritmo de Thompson para la concatenación. Precedencia media.
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
     * union() - Operador de expresión regular de menor precedencia. Algoritmo de
     * Thompson para la unión (o).
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

        // 2 nuevos estados y desplazamiento de m para evitar la repetición del último n
        // y el primer m
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

    public static List<Integer> findMatchingPositions(String input, String regex) {
        List<Integer> matchPositions = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            matchPositions.add(matcher.start());
        }

        return matchPositions;
    }

    public static int matchAFD(AFD afd, String input, int startIndex) {
        Set<Integer> currentStateSet = new HashSet<>(afd.initial_state);
        int currentIndex = startIndex;
        int inputLength = input.length();
        int matchLength = 0;

        while (currentIndex < inputLength) {
            char currentChar = input.charAt(currentIndex);

            int nextState = getNextState(afd, currentStateSet, currentChar);

            if (nextState == -1) {
                break;
            }

            currentStateSet = afd.states.get(nextState);
            matchLength++;
            currentIndex++;
        }

        // Check if the final state is reached
        if (afd.final_states.contains(currentStateSet)) {
            return matchLength;
        }

        return 0;
    }

    public static int getNextState(AFD afd, Set<Integer> currentStateSet, char symbol) {
        for (Trans t : afd.transitions) {
            if (currentStateSet.contains(t.state_from) && t.trans_symbol == symbol) {
                return t.state_to;
            }
        }
        return -1; // No valid transition found
    }

    /*
     * compile() - compila la expresión regular dada en un AFND utilizando el
     * Algoritmo de Construcción de Thompson. Implementará un modelo de pila de
     * compilador típico para simplificar el procesamiento de la cadena. Esto da
     * precedencia descendente a los caracteres a la derecha.
     */
    public static AFND compile(String regex) {

        if (!validRegEx(regex)) {
            System.out.println("Entrada de Expresión Regular Inválida:" + regex);
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

        
        System.out.println("Vista del AFND:");
        System.out.println("K=" + operands.peek().states);
        System.out.println("Sigma={a, b}");
        System.out.println("Delta:");
        operands.peek().display();
        System.out.println("s=q0");
        System.out.println("F=" + "[" + operands.peek().final_state + "]");
        
        return operands.pop();
    }

    public static class AFD {
        public List<Set<Integer>> states;
        public List<Trans> transitions;
        public Set<Integer> initial_state;
        public Set<Set<Integer>> final_states;

        public AFD() {
            this.states = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.initial_state = new HashSet<>();
            this.final_states = new HashSet<>();
        }

        public void display() {
            for (Trans t : transitions) {
                System.out.println("(" + t.state_from + ", " + t.trans_symbol + ", " + t.state_to + ")");
            }
        }
    }

    public static Set<Integer> epsilonClosure(Set<Integer> states, AFND afnd) {
        Set<Integer> closure = new HashSet<>(states);
        boolean changed;
        do {
            changed = false;
            for (Trans t : afnd.transitions) {
                if (t.trans_symbol == 'E' && closure.contains(t.state_from) && !closure.contains(t.state_to)) {
                    closure.add(t.state_to);
                    changed = true;
                }
            }
        } while (changed);

        return closure;
    }

    // Función para convertir un AFND en un AFD (versión corregida)
    public static AFD convertToAFD(AFND afnd) {
        AFD afd = new AFD();
        Queue<Set<Integer>> unprocessedStates = new LinkedList<>();
        Set<Set<Integer>> processedStates = new HashSet<>();

        // Calcula el cierre-épsilon del estado inicial del AFND
        Set<Integer> initialStateEpsilonClosure = epsilonClosure(new HashSet<>(Collections.singletonList(0)), afnd);

        // Agrega el estado inicial del AFD
        unprocessedStates.offer(initialStateEpsilonClosure);
        afd.states.add(initialStateEpsilonClosure);
        afd.initial_state = initialStateEpsilonClosure;

        while (!unprocessedStates.isEmpty()) {
            Set<Integer> currentStateSet = unprocessedStates.poll();
            processedStates.add(currentStateSet);

            for (char symbol = 'a'; symbol <= 'z'; symbol++) {
                Set<Integer> newStateSet = new HashSet<>();

                // Calcula el conjunto de estados alcanzados por el símbolo
                for (int state : currentStateSet) {
                    for (Trans t : afnd.transitions) {
                        if (t.state_from == state && t.trans_symbol == symbol) {
                            Set<Integer> nextStateEpsilonClosure = epsilonClosure(
                                    new HashSet<>(Collections.singletonList(t.state_to)), afnd);
                            newStateSet.addAll(nextStateEpsilonClosure);
                        }
                    }
                }

                if (!newStateSet.isEmpty()) {
                    afd.transitions.add(new Trans(afd.states.indexOf(currentStateSet), afd.states.size(), symbol));
                    afd.states.add(newStateSet);

                    if (!processedStates.contains(newStateSet) && !unprocessedStates.contains(newStateSet)) {
                        unprocessedStates.offer(newStateSet);
                    }
                }
            }
        }

        // Identifica los estados finales del AFD
        for (Set<Integer> stateSet : afd.states) {
            for (int state : stateSet) {
                if (afnd.final_state == state) {
                    afd.final_states.add(stateSet);
                    break;
                }
            }
        }
        System.out.println("Vista del AFD:");
        System.out.println("K=" + afd.states);
        System.out.println("Sigma={a, b}");
        System.out.println("Delta:");
        afd.display();
        System.out.println("s=" + afd.initial_state);
        System.out.println("F=" + afd.final_states);
           
        return afd;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String line;
        System.out.println("Ingrese una expresión regular con el " +
                "alfabeto ['a', 'z'] y E para el vacío " + "\n* para el cierre de Kleene" +
                "\nLos elementos sin nada entre ellos indican concatenación " +
                "\n| para unión \n\":QUIT\" para salir");

        while (sc.hasNextLine()) {
            System.out.println(
                    ":QUIT para salir, :AFND para ingresar expresion regular, :MATCH para comprobar unna cadena");
            line = sc.nextLine();
            if (line.equals(":QUIT"))
                break;
            if (line.equals(":AFND")) {
                // Convertir el AFND en un AFD y mostrarlo.
                System.out.println("Ingresa tu ER para AFND");
                AFND afnd = compile(sc.nextLine());

            }
            if (line.equals(":AFD")) {
                System.out.println("Ingresa tu ER para AFD");
                AFND afnd = compile(sc.nextLine());
                System.out.println("\n");
                AFD afd = convertToAFD(afnd);

            }
            if (line.equals(":MATCH")) {
                System.out.println("Ingresa tu ER");
                String regex = sc.nextLine();
                System.out.println("Ingresa la cadena de entrada:");
                String input = sc.nextLine();

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(input);

                List<Integer> matchPositions = new ArrayList<>();
                while (matcher.find()) {
                    matchPositions.add(matcher.start());
                }

                if (matchPositions.isEmpty()) {
                    System.out.println("No se encontraron coincidencias.");
                } else {
                    System.out.println("Las coincidencias comienzan en las siguientes posiciones:");
                    System.out.println(matchPositions);
                }
            }
        }       
    }
}