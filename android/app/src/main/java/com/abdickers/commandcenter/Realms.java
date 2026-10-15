package com.abdickers.commandcenter;

/** Fixed, reviewed destinations. No URL or script can be supplied through an Intent. */
public final class Realms {
    public static final String PERSONAL = "personal", WORK = "work";
    public static final String PERSONAL_URL = "https://abdickers.github.io/HomePage/";
    public static final String WORK_URL = "https://adams-middle-earth-command.spirit-of-76-1921.chatgpt.site/";
    private Realms() {}
    public static String normalize(String realm) { return WORK.equals(realm) ? WORK : PERSONAL; }
    public static String url(String realm) { return WORK.equals(normalize(realm)) ? WORK_URL : PERSONAL_URL; }
    public static String title(String realm) { return WORK.equals(normalize(realm)) ? "The White Tower" : "The Shire"; }
    /** Keep the private site's complete authentication and dashboard session in one browser context. */
    public static boolean usesBrowser(String realm) { return WORK.equals(normalize(realm)); }
}
