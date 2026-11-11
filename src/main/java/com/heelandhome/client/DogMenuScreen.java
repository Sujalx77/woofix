package com.heelandhome.client;

import com.heelandhome.net.DogActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class DogMenuScreen extends Screen {
    private EditBox nameBox;
    private String nameText = "";

    public DogMenuScreen() {
        super(Component.translatable("gui.heelandhome.title"));
    }

    public void refresh() {
        rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int w = 200;
        int x = width / 2 - w / 2;
        int y = Math.max(30, height / 2 - 85);

        addRenderableWidget(Button.builder(dogLabel(), b -> { cycleDog(); rebuildWidgets(); })
                .bounds(x, y, w, 20).build());
        y += 24;
        addRenderableWidget(Button.builder(homeLabel(), b -> { cycleHome(); rebuildWidgets(); })
                .bounds(x, y, w, 20).build());
        y += 30;

        addRenderableWidget(Button.builder(Component.translatable("gui.heelandhome.call"), b -> {
            send(DogActionPayload.CALL, "");
            onClose();
        }).bounds(x, y, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.heelandhome.send_home"), b -> {
            send(DogActionPayload.HOME, ClientState.selectedHome);
            onClose();
        }).bounds(x + 102, y, 98, 20).build());
        y += 30;

        nameBox = new EditBox(font, x, y, 124, 20, Component.empty());
        nameBox.setMaxLength(24);
        nameBox.setValue(nameText);
        nameBox.setHint(Component.translatable("gui.heelandhome.name_hint"));
        nameBox.setResponder(s -> nameText = s);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.heelandhome.save_home"), b -> {
            send(DogActionPayload.SET_HOME, nameText.trim());
            nameText = "";
            nameBox.setValue("");
        }).bounds(x + 128, y, 72, 20).build());
        y += 24;

        addRenderableWidget(Button.builder(Component.translatable("gui.heelandhome.link"), b ->
                send(DogActionPayload.LINK, ClientState.selectedHome)
        ).bounds(x, y, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.heelandhome.delete"), b -> {
            if (!ClientState.selectedHome.isEmpty()) send(DogActionPayload.DELETE_HOME, ClientState.selectedHome);
        }).bounds(x + 102, y, 98, 20).build());
        y += 30;

        addRenderableWidget(Button.builder(Component.translatable("gui.heelandhome.close"), b -> onClose())
                .bounds(x, y, w, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int top = Math.max(30, height / 2 - 85);
        g.drawCenteredString(font, title, width / 2, top - 18, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("gui.heelandhome.hint"), width / 2, top + 168, 0xAAAAAA);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!nameBox.isFocused() && ClientModEvents.MENU_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---- helpers

    private void send(int action, String name) {
        PacketDistributor.sendToServer(new DogActionPayload(action, name, ClientState.selectedDog));
    }

    private Component dogLabel() {
        Component n = ClientState.selectedDog.isEmpty()
                ? Component.translatable("gui.heelandhome.all_dogs")
                : Component.literal(ClientState.dogName(ClientState.selectedDog));
        return Component.translatable("gui.heelandhome.dog", n);
    }

    private Component homeLabel() {
        Component n = ClientState.selectedHome.isEmpty()
                ? Component.translatable("gui.heelandhome.home_default")
                : Component.literal(ClientState.selectedHome);
        return Component.translatable("gui.heelandhome.home", n);
    }

    private void cycleDog() {
        List<String> ids = new ArrayList<>();
        ids.add("");
        for (ClientState.Dog d : ClientState.dogs) ids.add(d.id());
        int i = ids.indexOf(ClientState.selectedDog);
        ClientState.selectedDog = ids.get((i + 1) % ids.size());
    }

    private void cycleHome() {
        List<String> names = new ArrayList<>();
        names.add("");
        names.addAll(ClientState.homes);
        int i = names.indexOf(ClientState.selectedHome);
        ClientState.selectedHome = names.get((i + 1) % names.size());
    }
}
