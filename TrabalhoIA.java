package TrabalhoIA;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

 // ==============================
    // 1. Método Principal e Leitura de Arquivos
    // ==============================

public class TrabalhoIA {
    public static void main(String[] args) throws UnsupportedEncodingException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Escolha o Nível de Dificuldade:");
        System.out.println("1 - Fácil ");
        System.out.println("2 - Médio");
        System.out.println("3 - Díficil");
        System.out.println("4 - Muito Difícil");
        System.out.println("5 - Impossível");
        System.out.println("Escolha o nível de dificuldade (1 - 5): ");
        int opcao = scanner.nextInt();
        scanner.nextLine(); // limpa o buffer

        String gridFile = "";
        String wordFile = "";
        String outputFile = "";
        String logFile = "";

        switch (opcao) {
            case 1:
                gridFile = "grid1.txt";
                wordFile = "lista_palavras.txt";
                outputFile = "output1.txt";
                logFile = "log1.txt";
                break;
            case 2:
                gridFile = "grid2.txt";
                wordFile = "lista_palavras.txt";
                outputFile = "output2.txt";
                logFile = "log2.txt";
                break;
            case 3:
                gridFile = "grid3.txt";
                wordFile = "lista_palavras.txt";
                outputFile = "output3.txt";
                logFile = "log3.txt";
                break;
            case 4: 
                gridFile = "grid4.txt";
                wordFile = "lista_palavras.txt";
                outputFile = "output4.txt";
                logFile = "log4.txt";
                break;
            case 5:     
                gridFile = "grid5.txt";
                wordFile = "lista_palavras.txt";
                outputFile = "output5.txt";
                logFile = "log5.txt";
                break;
            default:
                System.out.println("Opção inválida.");
                System.exit(1);
        }

        solveCrossword(gridFile, wordFile, outputFile, logFile, 600); // tempo limite = 600s
    }

    public static void solveCrossword(String gridFile, String wordFile, String outputFile, String logFile, int maxDuration) throws UnsupportedEncodingException {
        long startTime = System.currentTimeMillis();
        List<String> log = new ArrayList<>();
        log.add("Início: " + new Date(startTime));

        try {
            Object[] parsed = parseGrid(gridFile);
            List<String> grid = (List<String>) parsed[0];
            List<Slot> slots = (List<Slot>) parsed[1];
            log.add("Slots encontrados: " + slots.size());

            Map<Integer, List<String>> wordsByLength = loadWords(wordFile);

            List<int[]> constraints = findConstraints(slots);
            log.add("Restrições definidas: " + constraints.size());

            Map<Integer, List<int[]>> constraintMap = precomputeConstraints(constraints, slots.size());

            List<Set<String>> domains = new ArrayList<>();
            for (Slot s : slots) {
                domains.add(new HashSet<>(wordsByLength.getOrDefault(s.length, new ArrayList<>())));
            }

            Set<String> usedWords = new HashSet<>();
            Map<Integer, String> assignment = backtrack(new HashMap<>(), domains, slots, constraintMap, log, startTime, maxDuration, usedWords);

            if (assignment == null) {
                log.add("Nenhuma solução encontrada");
                System.out.println("Nenhuma solução encontrada");
            } else {
                log.add("Solução encontrada");
                List<String> filledGrid = fillGrid(grid, slots, assignment);
                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8"))) {
                    for (String row : filledGrid) pw.println(row);
                }
                log.add("Grade preenchida escrita em: " + outputFile);
            }
        } catch (Exception e) {
            log.add("Erro: " + e.getMessage());
            System.out.println("Erro: " + e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis();
            double elapsed = (endTime - startTime) / 1000.0;
            log.add("Término: " + new Date(endTime));
            log.add("Tempo total: " + String.format("%.2f", elapsed) + " segundos");
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile), "UTF-8"))) {
                for (String entry : log) pw.println(entry);
            } catch (FileNotFoundException e) {
                System.err.println("Não foi possível escrever o arquivo de log: " + e.getMessage());
            }
            System.out.println("Tempo total: " + String.format("%.2f", elapsed) + " segundos");
        }
    }

    // ==============================
    // 2. Leitura e Processamento de Arquivos
    // ==============================

    static class Slot {
        String direction; // "h" ou "v"
        int startRow, startCol, length;

        Slot(String direction, int startRow, int startCol, int length) {
            this.direction = direction;
            this.startRow = startRow;
            this.startCol = startCol;
            this.length = length;
        }

        @Override
        public String toString() {
            return direction + "_" + startRow + "_" + startCol + "_" + length;
        }
    }

     // Lê o grid de entrada e extrai todos os slots horizontais e verticais
    static Object[] parseGrid(String gridFile) throws IOException {
        List<String> grid = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(gridFile), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) grid.add(line.trim());
        }
        int rows = grid.size();
        int cols = grid.get(0).length();
        List<Slot> slots = new ArrayList<>();

        // Verifica horizontalmente
        for (int r = 0; r < rows; r++) {
            for (int[] slot : findSlots(grid.get(r))) {
                slots.add(new Slot("h", r, slot[0], slot[1]));
            }
        }

        // Verifica verticalmente
        for (int c = 0; c < cols; c++) {
            StringBuilder column = new StringBuilder();
            for (int r = 0; r < rows; r++) column.append(grid.get(r).charAt(c));
            for (int[] slot : findSlots(column.toString())) {
                slots.add(new Slot("v", slot[0], c, slot[1]));
            }
        }
        return new Object[]{grid, slots};
    }

    static Map<Integer, List<String>> loadWords(String wordFile) throws IOException {
        Map<Integer, List<String>> wordsByLength = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(wordFile), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String word = line.trim();
                wordsByLength.computeIfAbsent(word.length(), k -> new ArrayList<>()).add(word);
            }
        }
        return wordsByLength;
    }

    // ==============================
    // 3. Organização de Slots e Palavras
    // ==============================

    // Identifica todos os intervalos de interrogações consecutivas com tamanho >= 2
    static List<int[]> findSlots(String line) {
        List<int[]> slots = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            if (line.charAt(i) == '?') {
                int j = i;
                while (j < line.length() && line.charAt(j) == '?') j++;
                if (j - i >= 2) slots.add(new int[]{i, j - i});
                i = j;
            } else {
                i++;
            }
        }
        return slots;
    }

    static List<int[]> findConstraints(List<Slot> slots) {
        List<int[]> constraints = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                Slot s1 = slots.get(i);
                Slot s2 = slots.get(j);
                if (s1.direction.equals(s2.direction)) continue;
                Slot h = s1.direction.equals("h") ? s1 : s2;
                Slot v = s1.direction.equals("v") ? s1 : s2;
                if (v.startRow <= h.startRow && h.startRow < v.startRow + v.length &&
                        h.startCol <= v.startCol && v.startCol < h.startCol + h.length) {
                    int indexH = v.startCol - h.startCol;
                    int indexV = h.startRow - v.startRow;
                    constraints.add(new int[]{i, j, indexH, indexV});
                }
            }
        }
        return constraints;
    }

    static Map<Integer, List<int[]>> precomputeConstraints(List<int[]> constraints, int numSlots) {
        Map<Integer, List<int[]>> constraintMap = new HashMap<>();
        for (int i = 0; i < numSlots; i++) constraintMap.put(i, new ArrayList<>());
        for (int[] c : constraints) {
            int s1 = c[0], s2 = c[1], index1 = c[2], index2 = c[3];
            constraintMap.get(s1).add(new int[]{s2, index1, index2});
            constraintMap.get(s2).add(new int[]{s1, index2, index1});
        }
        return constraintMap;
    }

    // Verifica se uma palavra pode ser atribuída a um slot considerando os vizinhos já preenchidos
    static boolean isConsistent(int slotId, String word, Map<Integer, String> assignment, Map<Integer, List<int[]>> constraintMap) {
        for (int[] c : constraintMap.get(slotId)) {
            int otherSlot = c[0], index1 = c[1], index2 = c[2];
            if (assignment.containsKey(otherSlot)) {
                if (word.charAt(index1) != assignment.get(otherSlot).charAt(index2)) return false;
            }
        }
        return true;
    }

    // Forward checking
    static boolean forwardCheck(int slotId, String word, List<Set<String>> domains, Map<Integer, List<int[]>> constraintMap, Map<Integer, List<String>> removed) {
        for (int[] constraint : constraintMap.get(slotId)) {
            int otherSlot = constraint[0], index1 = constraint[1], index2 = constraint[2];
            Set<String> domain = domains.get(otherSlot);
            List<String> toRemove = new ArrayList<>();
            for (String w : domain) {
                if (w.charAt(index2) != word.charAt(index1)) {
                    toRemove.add(w);
                }
            }
            // Ajuda de IA - remove palavras inconsistentes
            if (!toRemove.isEmpty()) {
                removed.computeIfAbsent(otherSlot, k -> new ArrayList<>()).addAll(toRemove);
                domain.removeAll(toRemove);
                if (domain.isEmpty()) return false;
            }
        }
        return true;
    }

    static void restoreDomains(List<Set<String>> domains, Map<Integer, List<String>> removed) {
        for (Map.Entry<Integer, List<String>> entry : removed.entrySet()) {
            domains.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    static Map<Integer, String> backtrack(Map<Integer, String> assignment, List<Set<String>> domains, List<Slot> slots, Map<Integer, List<int[]>> constraintMap, List<String> log, long startTime, int maxDuration, Set<String> usedWords) {
        if (System.currentTimeMillis() - startTime > maxDuration * 1000) {
            throw new RuntimeException("Execution exceeded time limit of " + maxDuration + " seconds");
        }
        if (assignment.size() == slots.size()) return assignment;

        // MRV + grau: escolhe variável com menor domínio e mais restrições
        // Ajuda de IA
        Set<Integer> unassigned = new HashSet<>();
        for (int i = 0; i < slots.size(); i++) if (!assignment.containsKey(i)) unassigned.add(i);
        int slotId = Collections.min(unassigned, Comparator
            .comparingInt((Integer s) -> domains.get(s).size())
            .thenComparingInt(s -> -constraintMap.get(s).size()));

        // LCV: ordena palavras por menor comprimento
        List<String> values = new ArrayList<>(domains.get(slotId));
        values.sort(Comparator.comparingInt(String::length));

        for (String word : values) {
            if (usedWords.contains(word)) continue;
            if (isConsistent(slotId, word, assignment, constraintMap)) {
                log.add("Assigning '" + word + "' to slot " + slotId + " (" + slots.get(slotId) + ")");
                assignment.put(slotId, word);
                usedWords.add(word);
                Map<Integer, List<String>> removed = new HashMap<>();
                if (forwardCheck(slotId, word, domains, constraintMap, removed)) {
                    Map<Integer, String> result = backtrack(new HashMap<>(assignment), domains, slots, constraintMap, log, startTime, maxDuration, usedWords);
                    if (result != null) return result;
                }
                log.add("Backtracking from slot " + slotId);
                assignment.remove(slotId);
                usedWords.remove(word);
                restoreDomains(domains, removed);
            }
        }
        return null;
    }

    static List<String> fillGrid(List<String> grid, List<Slot> slots, Map<Integer, String> assignment) {
        List<char[]> filled = grid.stream().map(String::toCharArray).collect(Collectors.toList());
        for (Map.Entry<Integer, String> entry : assignment.entrySet()) {
            int slotId = entry.getKey();
            String word = entry.getValue();
            Slot slot = slots.get(slotId);
            if (slot.direction.equals("h")) {
                for (int i = 0; i < word.length(); i++) {
                    filled.get(slot.startRow)[slot.startCol + i] = word.charAt(i);
                }
            } else {
                for (int i = 0; i < word.length(); i++) {
                    filled.get(slot.startRow + i)[slot.startCol] = word.charAt(i);
                }
            }
        }
        return filled.stream().map(String::new).collect(Collectors.toList());
    }

}
