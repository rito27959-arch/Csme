package com.csme.desktop.ui.users;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.Overlay;
import com.csme.desktop.ui.common.Ui;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.AgencyDtos.AgencyDto;
import com.csme.shared.dto.AuthDtos.AgencyRef;
import com.csme.shared.dto.DepartmentDtos.DepartmentDto;
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

    record Choices(List<RoleDto> roles, List<AgencyDto> agencies, List<DepartmentDto> departments) {
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
            boolean grantable = r.permissions().stream().allMatch(p -> ctx.session().user().can(p));
            if (!grantable && !box.isSelected()) {
                box.setDisable(true);
                box.setTooltip(new Tooltip("Ce rôle accorde des permissions que vous ne possédez pas."));
            }
            if (r.description() != null && box.getTooltip() == null) {
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

        javafx.scene.control.ComboBox<DepartmentDto> department = new javafx.scene.control.ComboBox<>();
        department.getItems().add(null);
        department.getItems().addAll(choices.departments().stream()
                .filter(d -> d.active() || (!creating && existing.department() != null && d.id().equals(existing.department().id())))
                .toList());
        department.setMaxWidth(Double.MAX_VALUE);
        javafx.util.Callback<javafx.scene.control.ListView<DepartmentDto>, javafx.scene.control.ListCell<DepartmentDto>> cells =
                l -> new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(DepartmentDto d, boolean empty) {
                        super.updateItem(d, empty);
                        setText(empty ? null : d == null ? "Aucun département" : d.name() + " — " + d.agency().name());
                    }
                };
        department.setCellFactory(cells);
        department.setButtonCell(cells.call(null));
        if (!creating && existing.department() != null) {
            department.getItems().stream().filter(d -> d != null && d.id().equals(existing.department().id()))
                    .findFirst().ifPresent(department::setValue);
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
        if (!choices.departments().isEmpty()) {
            form.field("departmentId", "Département", department);
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
                        password.getText(), roleIds, agencyIds, departmentId(department));
                execute(() -> ctx.api().createUser(request), this::done);
            } else {
                UpdateUserRequest request = new UpdateUserRequest(fullName.getText(), jobTitle.getText(), roleIds, agencyIds,
                        departmentId(department));
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

    private static UUID departmentId(javafx.scene.control.ComboBox<DepartmentDto> box) {
        return box.getValue() == null ? null : box.getValue().id();
    }

    private static Set<UUID> selected(Map<UUID, CheckBox> boxes) {
        return boxes.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

}
