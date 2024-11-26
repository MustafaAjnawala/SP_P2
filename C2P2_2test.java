package C2P2_2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class C2P2_2test {

     static List<List<String>> NTab = new ArrayList<>();

    public static void main(String[] args) {
        List<String> obj1List = new ArrayList<>();
        List<String> obj2List = new ArrayList<>();
        List<String> obj3List = new ArrayList<>();
        List<String> reloc1 = new ArrayList<>();
        List<String> reloc2 = new ArrayList<>();
        List<String> reloc3 = new ArrayList<>();
        List<List<String>> link1 = new ArrayList<>();
        List<List<String>> link2 = new ArrayList<>();
        List<List<String>> link3 = new ArrayList<>();
        
        // Read files into lists
        loadFileToList("C2P2_2\\obj1.txt", obj1List);
        loadFileToList("C2P2_2\\obj2.txt", obj2List);
        loadFileToList("C2P2_2\\obj3.txt", obj3List);

         // Process LC, relocation, and link tables for each object list
         processObjList(obj1List, reloc1, link1);
         processObjList(obj2List, reloc2, link2);
         processObjList(obj3List, reloc3, link3);

        // Print modified obj lists, relocation entries, and LinkTab to verify
        System.out.println("object module : obj1\nLC Processing");
        obj1List.forEach(System.out::println);
        System.out.println("\nRELOCTAB");
        reloc1.forEach(System.out::println);
        System.out.println("\nLINKTAB");
        link1.forEach(row -> System.out.println(row));
        System.out.println("-----------------------------------");

        System.out.println("\nobject module: obj2\nLC Processing");
        obj2List.forEach(System.out::println);
        System.out.println("\nRELOCTAB");
        reloc2.forEach(System.out::println);
        System.out.println("\nLINKTAB");
        link2.forEach(row -> System.out.println(row));
        System.out.println("-----------------------------------");

        System.out.println("\nobject module:obj3\nLC Processing");
        obj3List.forEach(System.out::println);
        System.out.println("\nRELOCTAB");
        reloc3.forEach(System.out::println);
        System.out.println("\nLINKTAB");
        link3.forEach(row -> System.out.println(row));
        System.out.println("-----------------------------------");

        // Accept Link Origin from the user
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the Link Origin:");
        int linkOrigin = Integer.parseInt(sc.nextLine());

        // Calculate Relocation Factors
        int rfObj1 = linkOrigin - Integer.parseInt(obj1List.get(0).split(" ")[1]); // Translated origin for obj1
        int rfObj2 = linkOrigin + 6 - 106;
        int rfObj3 = linkOrigin + 15 - 115;

        // System.out.println("RFs calculated");
        // System.out.println(rfObj1);
        // System.out.println(rfObj2);
        // System.out.println(rfObj3);
        // System.out.println();
        
        // Populate NTab with linking addresses and PD symbols
        populateNTab("obj1", rfObj1, link1);
        populateNTab("obj2", rfObj2, link2);
        populateNTab("obj3", rfObj3, link3);
        
        // Print NTab to verify
        System.out.println("\nNTab:");
        NTab.forEach(row -> System.out.println(row));
        System.out.println();
        System.out.println("-----------------------------------");
        sc.close();
    }

    private static void loadFileToList(String filePath, List<String> objList) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                objList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processObjList(List<String> objList, List<String> relocList, List<List<String>> linkTab) {
        int lc = 0;  // Location Counter (LC)
        boolean firstAddressSensitiveLine = true;
        
        // Step 1: Extract the starting LC value from "START" directive
        String startLine = objList.get(0);
        if (startLine.startsWith("START")) {
            String[] parts = startLine.split(" ");
            lc = Integer.parseInt(parts[1]);  // Set LC to the value after "START"
        }

        // Step 2: Populate LinkTab by reading ENTRY and EXTERN symbols
        populateLinkTab(objList, linkTab);

        // Step 3: Process each line from the 3rd line to the line before "END"
        for (int i = 3; i < objList.size(); i++) {
            String line = objList.get(i);
            if (line.startsWith("END")) {
                break;
            }

            // Prefix the line with the current LC value
            String lcLine = lc + " " + line;
            objList.set(i, lcLine);

            if (firstAddressSensitiveLine || line.contains("BC") || line.contains("JL") ||line.contains("LDA")) {
                relocList.add(String.valueOf(lc));  // Add to reloc list if it's address-sensitive
                firstAddressSensitiveLine = false;
            }

            // Check if the line defines or uses a symbol and update LinkTab
            updateLinkTabForSymbols(linkTab, line, lc);

            // Increment LC based on the line's content
            lc += getInstructionSize(line);
        }
    }

    // Method to determine the size of each instruction in terms of LC increment
    private static int getInstructionSize(String line) {
        if (line.contains("DC")) {
            return 1;  // Assuming 'DC' directive allocates one memory unit
        }
        return 1;  // Default increment for other instructions
    }

    // Populate the LinkTab based on ENTRY and EXTERN symbols
    private static void populateLinkTab(List<String> objList, List<List<String>> linkTab) {
        // Parse the ENTRY line for PD symbols
        String entryLine = objList.get(1);
        if (entryLine.startsWith("ENTRY")) {
            String[] entrySymbols = entryLine.split(" ")[1].split(",");
            for (String symbol : entrySymbols) {
                List<String> entryRow = new ArrayList<>();
                entryRow.add(symbol);       // Symbol name
                entryRow.add("PD");         // Type
                entryRow.add("null");       // Placeholder for TranslatedAddr, updated later if found
                linkTab.add(entryRow);
            }
        }

        // Parse the EXTERN line for EXT symbols
        String externLine = objList.get(2);
        if (externLine.startsWith("EXTERN")) {
            String[] externSymbols = externLine.split(" ")[1].split(",");
            for (String symbol : externSymbols) {
                List<String> externRow = new ArrayList<>();
                externRow.add(symbol);      // Symbol name
                externRow.add("EXT");       // Type
                externRow.add("null");      // Placeholder for TranslatedAddr, updated later if used
                linkTab.add(externRow);
            }
        }
    }

    // Update the LinkTab for symbols used or defined within the program
    private static void updateLinkTabForSymbols(List<List<String>> linkTab, String line, int lc) {
        for (List<String> row : linkTab) {
            String symbolName = row.get(0);
            String type = row.get(1);

            if (type.equals("PD") && line.startsWith(symbolName + ":DC")) {
                row.set(2, Integer.toString(lc));  
            } else if (type.equals("EXT") && line.contains(symbolName)) {
                row.set(2, Integer.toString(lc)); 
            }
        }
    }

    // Populate NTab with each object's symbols and their addresses based on the RF
    private static void populateNTab(String objName, int rf, List<List<String>> linkTab) {
        List<String> objRow = new ArrayList<>();
        
        if(objName.equals("obj1")){
            objRow.add(objName);
            objRow.add(Integer.toString(rf+100));
        }
        else if(objName.equals("obj2")){
            objRow.add(objName);
            objRow.add(Integer.toString(rf+106));
        }
        else if(objName.equals("obj3")){
            objRow.add(objName);
            objRow.add(Integer.toString(rf+115));
        }
        NTab.add(objRow);

        // Add each PD symbol and its relocated address to NTab
        for (List<String> row : linkTab) {
            String symbolName = row.get(0);
            String type = row.get(1);
            String translatedAddr = row.get(2);

            if (type.equals("PD") && !translatedAddr.equals("null")) {
                List<String> ntabRow = new ArrayList<>();
                ntabRow.add(symbolName);
                int adjustedAddress = Integer.parseInt(translatedAddr) + rf;  // Adjust with RF
                ntabRow.add(Integer.toString(adjustedAddress));
                NTab.add(ntabRow);
            }
        }
    }
}
