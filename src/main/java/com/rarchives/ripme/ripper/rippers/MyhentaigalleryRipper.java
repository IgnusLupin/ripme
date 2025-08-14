package com.rarchives.ripme.ripper.rippers;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import java.net.URISyntaxException;



public class MyhentaigalleryRipper extends AbstractHTMLRipper {

    private static final String BASE = "https://myhentaigallery.com/";
    private static final String BROWSER_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    public MyhentaigalleryRipper(URL url) throws IOException {
        super(url);
    }

    @Override
    public String getHost() {
        return "myhentaigallery";
    }

    @Override
    public String getDomain() {
        return "myhentaigallery.com";
    }

    // ONLY /g/ID (digits). Exactly one capture group.
    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("^https?://myhentaigallery\\.com/g/([0-9]+)/?$");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException(
            "Expected myhentaigallery.com URL format: myhentaigallery.com/g/ID - got " + url + " instead"
        );
    }

    // Fetch the page like a real browser (kills 403 on HTML)
    @Override
    public Document getFirstPage() throws IOException {
        return Http.url(url)
            .referrer(BASE)
            .userAgent(BROWSER_UA)
            .header("Accept-Language", "en-US,en;q=0.9")
            .get();
    }

    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> result = new ArrayList<>();
        for (Element el : doc.select(".comic-thumb > img")) {
            // Get an absolute, already-encoded URL (handles spaces & quotes)
            String src = el.absUrl("src");

            // Convert thumbs -> originals
            src = src.replace("/thumbnail/", "/original/").replace("thumbnail", "original");

            // Extra safety: if any strays remain, encode space + apostrophe
            if (src.indexOf(' ') >= 0 || src.indexOf('\'') >= 0) {
                src = src.replace(" ", "%20").replace("'", "%27");
            }

            result.add(src);
        }
        return result;
    }


    @Override
    public String getAlbumTitle(URL url) throws MalformedURLException, URISyntaxException {
        try {
            Document doc = getFirstPage();  // you already override this with UA/Referer

            Element h1 = doc.selectFirst(".comic-description h1");
            String title = (h1 != null)
                ? h1.text().trim()
                : doc.title()
                    .replaceFirst("\\s*Hentai Comic\\s*-\\s*My Hentai Gallery\\s*$", "")
                    .trim();

            // FQN avoids any import weirdness
            title = com.rarchives.ripme.utils.Utils.filesystemSafe(title);

            return String.format("%s_%s", getHost(), title);
        } catch (IOException e) {
            // Fallback to default (host_GID)
            return super.getAlbumTitle(url);
        }
    }   




    // Keep the simple downloader (no cookies ⇒ no index errors)
    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
        // If images 403 later, switch to a referer-aware overload your build supports, e.g.:
        // addURLToDownload(url, getPrefix(index), "", this.url.toString(), null /*cookies*/);
        // or: addURLToDownload(url, getPrefix(index), "", this.url.toString(), null, null);
    }
}
