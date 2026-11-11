package com.csme.desktop.ui.users;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AuthDtos.AgencyRef;
import com.csme.shared.dto.UserDtos.CreateUserRequest;
import com.csme.shared.dto.UserDtos.RoleDto;
import com.csme.shared.dto.UserDtos.RoleRef;
import com.csme.shared.dto.UserDtos.UpdateUserRequest;
import com.csme.shared.dto.UserDtos.UserDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Feuille de création / modification d'un utilisateur. */
final class UserEditor extends ViewModel {

    record Choices(List<RoleDto> roles, List<AgencyDto> agencies) {
    }

    private final AppContext ctx;
    private final Overlay overlay;
    private final UserDto existing;
    private final Choices choices;
    private final Consumer<UserDto> onSaved;

    UserEditor(AppContext ctx, Overlay overlay, UserDto existing, Choices choices, Consumer<UserDto> onSaved) {
        this.ctx = ctx;
        this.overlay = overlay;
        this.existing = existing;
        this.choices = choices;
        this.onSaved = onSaved;
    }

    void show() {
        boolean creating = existing == null;
        TextField fullName = new TextField(creating ? "" : existing.fullName());
        TextField jobTitle = new TextField(creating || existing.jobTitle() == null ? "" : existing.jobTitle());
        TextField email = new TextField();
        PasswordField password = new PasswordField();

        Set<UUID> currentRoles = creating ? Set.of()
                : existing.roles().stream().map(RoleRef::id).collect(Collectors.toSet());
        Map<UUID, CheckBox> roleBoxes = new LinkedHashMap<>();
        FlowPane roles = new FlowPane(12, 8);
        for (RoleDto r : choices.roles()) {
            CheckBox box = new CheckBox(r.name());
            box.setSelected(currentRoles.contains(r.id()));
            if (r.description() != null) {
                box.setTooltip(new Tooltip(r.description()));
            }
            roleBoxes.put(r.id(), box);
            roles.getChildren().add(box);
        }

        Set<UUID> currentAgencies = creating ? Set.of()
                : existing.agencies().stream().map(AgencyRef::id).collect(Collectors.toSet());
        Map<UUID, CheckBox> agencyBoxes = new LinkedHashMap<>();
        FlowPane agencies = new FlowPane(12, 8);
        for (AgencyDto a : choices.agencies()) {
            CheckBox box = new CheckBox(a.name());
            box.setSelected(currentAgencies.contains(a.id()));
            box.setDisable(!a.active() && !box.isSelected());
            agencyBoxes.put(a.id(), box);
            agencies.getChildren().add(box);
        }

        Ui.Form form = new Ui.Form();
        form.field("fullName", "Nom complet", fullName);
        if (creating) {
            form.field("email", "Adresse électronique", email)
                    .field("initialPassword", "Mot de passe initial", password);
        }
        form.field("jobTitle", "Poste", jobTitle)
                .field("roleIds", "Rôles", roles);
        if (!choices.agencies().isEmpty()) {
            form.field("agencyIds", "Agences autorisées", agencies);
        }
        form.bindErrors(fieldErrorsProperty());

        Button cancel = new Button("Annuler");
        cancel.setCancelButton(true);
        cancel.setOnAction(e -> overlay.close());
        Button save = Ui.primary(creating ? "Créer le compte" : "Enregistrer", null);
        save.setDefaultButton(true);
        save.disableProperty().bind(busyProperty());
        save.setOnAction(e -> {
            Set<UUID> roleIds = selected(roleBoxes);
            Set<UUID> agencyIds = selected(agencyBoxes);
            if (creating) {
                CreateUserRequest request = new CreateUserRequest(email.getText(), fullName.getText(), jobTitle.getText(),
                        password.getText(), roleIds, agencyIds);
                execute(() -> ctx.api().createUser(request), this::done);
            } else {
                UpdateUserRequest request = new UpdateUserRequest(fullName.getText(), jobTitle.getText(), roleIds, agencyIds);
                execute(() -> ctx.api().updateUser(existing.id(), request), this::done);
            }
        });

        ScrollPane scroll = new ScrollPane(new VBox(6, Ui.errorBanner(errorProperty()), form.node()));
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(430);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        overlay.sheet(creating ? "Nouvel utilisateur" : "Modifier " + existing.fullName(), scroll, cancel, save);
    }

    private void done(UserDto saved) {
        overlay.close();
        overlay.success(existing == null ? "Compte créé pour " + saved.fullName() + "." : "Modifications enregistrées.");
        onSaved.accept(saved);
    }

    private static Set<UUID> selected(Map<UUID, CheckBox> boxes) {
        return boxes.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
