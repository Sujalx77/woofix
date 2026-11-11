package com.heelandhome.client;

import java.util.ArrayList;
import java.util.List;

public final class ClientState {
    public record Dog(String id, String name, String home) {}

    public static List<String> homes = new ArrayList<>();
    public static List<Dog> dogs = new ArrayList<>();
    /** "" = all dogs */
    public static String selectedDog = "";
    /** "" = the dog's linked home, or the first home */
    public static String selectedHome = "";

    private ClientState() {}

    public static void reset() {
        homes = new ArrayList<>();
        dogs = new ArrayList<>();
        selectedDog = "";
        selectedHome = "";
    }

    public static void validate() {
        if (!selectedDog.isEmpty() && dogs.stream().noneMatch(d -> d.id().equals(selectedDog))) selectedDog = "";
        if (!selectedHome.isEmpty() && homes.stream().noneMatch(h -> h.equalsIgnoreCase(selectedHome))) selectedHome = "";
    }

    public static String dogName(String id) {
        for (Dog d : dogs) if (d.id().equals(id)) return d.name();
        return "?";
    }
}
