package com.csme.desktop.ui.login;

import com.csme.desktop.AppContext;
import com.csme.desktop.ui.common.ViewModel;
import com.csme.shared.dto.AuthDtos.TokenResponse;
import java.util.function.Consumer;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class LoginViewModel extends ViewModel {

    private final AppContext ctx;
    private final StringProperty email = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");

    public LoginViewModel(AppContext ctx) {
        this.ctx = ctx;
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public BooleanBinding canSubmit() {
        return Bindings.createBooleanBinding(
                () -> !busyProperty().get() && !email.get().isBlank() && !password.get().isEmpty(),
                busyProperty(), email, password);
    }

    public void login(Consumer<TokenResponse> onSuccess) {
        String e = email.get().strip();
        String p = password.get();
        execute(() -> ctx.api().login(e, p), tokens -> {
            password.set("");
            ctx.session().open(tokens);
            onSuccess.accept(tokens);
        }, failure -> password.set(""));
    }
}
