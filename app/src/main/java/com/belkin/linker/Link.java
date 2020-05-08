package com.belkin.linker;

import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Link {
    private static String CLASS_LOG_TAG = "Link";

    private final String HEADER_PLACEHOLDER = "Cannot Open Page";

    private String header;
    private String host;
    private String datetime;
    private String imageUrl;
    private String url;
    private long id;


    Link(String url, LinkToListItemAdapter adapter, int position) {
        this.url = url;
        new DownloadTask(adapter, position).execute();
    }

    private class DownloadTask extends AsyncTask<Void, Void, Void> {
        private LinkToListItemAdapter adapter;
        private int pos;

        DownloadTask(LinkToListItemAdapter adapter, int pos) {
            this.adapter = adapter;
            this.pos = pos;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setLoading();
            adapter.notifyItemChanged(pos);
        }

        @Override
        protected Void doInBackground(Void... params) {
            setHost();
            setDatetime();
            setImageAndHeader();
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            adapter.animateLoading(pos);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            adapter.notifyItemChanged(pos);
            DataBase.writeToDataBase(getInstance());
        }
    }

    private void setLoading() {
        header = "Loading..";
        datetime = "";
        host = "";
    }

    private Link getInstance() {
        return this;
    }

    Link(long id, String url, String host, String header, String imageUrl, String datetime) {
        this.id = id;
        this.url = url;
        this.host = host;
        this.header = header;
        this.imageUrl = imageUrl;
        this.datetime = datetime;
    }

    //getters
    String getImageUrl() {
        return imageUrl;
    }

    String getHeader() {
        return header;
    }

    String getHost() {
        return host;
    }

    String getDatetime() {
        return datetime;
    }

    String getUrl() {
        return url;
    }

    long getId() {
        return id;
    }


    //setters
    void setId(long id) {
        this.id = id;
    }

    private void setHeader(Document doc) {
        Log.i(CLASS_LOG_TAG, "setHeader(Document) method called");

        //1st priority: <meta property="og:title" content="X" /> where X is page title
        Elements els = doc.select("meta").select("[property=og:title]");
        if (els.size() != 0) {
            header = els.get(0).attr("content");
            Log.i(CLASS_LOG_TAG, "setHeader() found 1st priority header");
            return;
        }

        //2nd priority: <title>X</title> where X is page title
        els = doc.select("title");
        if (els.size() != 0) {
            header = els.get(0).text();
            Log.i(CLASS_LOG_TAG, "setHeader() found 2nd priority header");
            return;
        }

        //3rd priority: <h1>X</h1> where X is page title
        els = doc.select("h1");
        if (els.size() != 0) {
            header = els.get(0).text();
            Log.i(CLASS_LOG_TAG, "setHeader() found 3rd priority header");
            return;
        }

        //4th priority: host
        header = getHost();
        Log.i(CLASS_LOG_TAG, "setHeader() did not find any header, using host name instead");
    }

    private void setImage(Document doc) {
        Log.i(CLASS_LOG_TAG, "setImage(Document) method called");

        //1st priority: <meta property="og:image" content="X" /> where X is image url and X can't contain "favicon" char sequence
        Elements els = doc.select("meta").select("[property=og:image]").select("[content~=^((?!favicon).)*$]");
        if (els.size() != 0) {
            imageUrl = els.get(0).attr("content");
            Log.i(CLASS_LOG_TAG, "setImage() found 1st priority image");
            return;
        }

        //2nd priority: <img src="X"/> where X is image url and X can't contain "favicon" char sequence
        els = doc.select("img").select("[src~=.(png|jpe?g)]").select("[src~=^((?!favicon).)*$]");
        if (els.size() != 0) {
            imageUrl = els.get(0).attr("src");
            Log.i(CLASS_LOG_TAG, "setImage() found 2nd priority image");
            return;
        }

        //3rd priority: <link href="X"/> where X is image url and X can't contain "favicon" char sequence
        els = doc.select("link").select("[href~=.(png|jpe?g)]").select("[href~=^((?!favicon).)*$]");
        if (els.size() != 0) {
            imageUrl = els.get(0).attr("href");
            Log.i(CLASS_LOG_TAG, "setImage() found 3rd priority image");
            return;
        }

        //4th priority: <meta property="og:image" content="X" /> where X is image url and X can contain "favicon" char sequence
        els = doc.select("meta").select("[property=og:image]");
        if (els.size() != 0) {
            imageUrl = els.get(0).attr("content");
            Log.i(CLASS_LOG_TAG, "setImage() found 4th priority image");
            return;
        }

        //5th priority: <img src="X"/> where X is image url and X can't contain "favicon" char sequence
        els = doc.select("img").select("[src~=.(png|jpe?g)]");
        if (els.size() != 0) {
            imageUrl = els.get(0).attr("src");
            Log.i(CLASS_LOG_TAG, "setImage() found 5th priority image");
            return;
        }

        //6th priority: <link href="X"/> where X is image url and X can't contain "favicon" char sequence
        els = doc.select("link").select("[href~=.(png|jpe?g)]");
        if (els.size() != 0) {
            imageUrl = els.get(0).attr("href");
            Log.i(CLASS_LOG_TAG, "setImage() found 6th priority image");
            return;
        }

        Log.i(CLASS_LOG_TAG, "setImage() did not find any image, using placeholder");
    }


    private void setImageAndHeader() {
        Log.i(CLASS_LOG_TAG, "setImageAndHeader() method called");

        header = HEADER_PLACEHOLDER;
        if (url != null) {
            Log.i(CLASS_LOG_TAG, "Reading web page from URL: " + url);
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Chrome/4.0.249.0 Safari/532.5")
                        .referrer("http://www.google.com")
                        .get();

                setImage(doc);
                setHeader(doc);
                Log.i(CLASS_LOG_TAG, "Header is set: " + header);
                Log.i(CLASS_LOG_TAG, "Image url is set: " + imageUrl);
            } catch (IOException e) {
                Log.e(CLASS_LOG_TAG, "Error parsing image and header from url. " + e.getMessage());
            }

        } else {
            Log.e(CLASS_LOG_TAG, "Error parsing image and header from url. Url is null");
        }

    }

    private void setHost() {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            host = domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void setDatetime() {
        datetime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    @Override
    public String toString() {
        return "Id = " + id + " " +
                "URL = " + url + " " +
                "Host = " + host + " " +
                "Header = " + header + " " +
                "Datetime = " + datetime;
    }
}
