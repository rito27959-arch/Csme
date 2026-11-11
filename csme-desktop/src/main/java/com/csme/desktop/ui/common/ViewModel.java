package com.csme.desktop.ui.common;

import com.csme.desktop.api.ApiException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Base des ViewModels (MVVM) : état « occupé », message d'erreur et erreurs par champ.
 * Une seule opération à la fois : les doubles soumissions sont ignorées.
 */
public abstract class ViewModel {

    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final StringProperty error = new SimpleStringProperty();
    private final ObjectProperty<Map<String, String>> fieldErrors = new SimpleObjectProperty<>(Map.of());

    public BooleanProperty busyProperty() {
        return busy;
    }

    public StringProperty errorProperty() {
        return error;
    }

    public ObjectProperty<Map<String, String>> fieldErrorsProperty() {
        return fieldErrors;
    }

    protected <T> boolean execute(Callable<T> work, Consumer<T> onSuccess) {
        return execute(work, onSuccess, e -> { });
    }

    protected <T> boolean execute(Callable<T> work, Consumer<T> onSuccess, Consumer<ApiException> onFailure) {
        if (busy.get()) {
            return false;
        }
        busy.set(true);
        error.set(null);
        fieldErrors.set(Map.of());
        Async.run(work, result -> {
            busy.set(false);
            onSuccess.accept(result);
        }, failure -> {
            busy.set(false);
            ApiException api = failure instanceof ApiException e ? e
                    : new ApiException(-1, "CLIENT_ERROR", "Erreur inattendue : " + failure.getMessage(), Map.of());
            error.set(api.getMessage());
            fieldErrors.set(api.fieldErrors());
            onFailure.accept(api);
        });
        return true;
    }
}
