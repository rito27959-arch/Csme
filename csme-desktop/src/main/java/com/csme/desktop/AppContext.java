package com.csme.desktop;

import com.csme.desktop.api.CsmeApi;
import com.csme.desktop.config.AppConfig;
import com.csme.desktop.session.Session;
import com.csme.desktop.ui.common.ThemeService;

/** Services partagés de l'application (injection manuelle, sans conteneur). */
public record AppContext(AppConfig config, Session session, CsmeApi api, ThemeService theme) {
}
