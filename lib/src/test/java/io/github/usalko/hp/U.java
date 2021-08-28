package io.github.usalko.hp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class _Url {

    final char[] scheme;
    final char[] user;
    final char[] password;
    final char[] host;
    final char[] port;
    final boolean isRootPath;
    final char[][] path;
    final char[][] query;
    final char[] fragment;

    private _Url(char[] scheme, char[] user, char[] password, char[] host, char[] port,
            boolean isRootPath, char[][] path, char[][] query, char[] fragment) {
        this.scheme = scheme;
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = port;
        this.isRootPath = isRootPath;
        this.path = path;
        this.query = query;
        this.fragment = fragment;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(this.length());
        result.append(scheme).append(':');
        if (user.length > 0 || host.length > 0 || password.length > 0 || port.length > 0) {
            result.append("//");
        }
        if (user.length > 0 && password.length > 0) {
            result.append(user).append(':').append(password).append('@');
        } else if (user.length > 0) {
            result.append(user).append('@');
        } else if (password.length > 0) {
            result.append(password).append('@');
        }

        if (host.length > 0) {
            result.append(host);
        }
        if (port.length > 0) {
            result.append(':').append(port);
        } else if (password.length > 0) {
            result.append(password).append('@');
        }

        if (isRootPath) {
            result.append('/');
        }
        if (path.length > 0) {
            result.append(Arrays.stream(path).map(String::valueOf).collect(
                    Collectors.joining("/")));
        }

        if (query.length > 0) {
            result.append(Arrays.stream(query).map(String::valueOf).collect(
                    Collectors.joining("&")));
        }

        if (fragment.length > 0) {
            result.append('#').append(fragment);
        }

        return result.toString();
    }

    private int length() {
        return scheme.length + 1 + user.length + 1 + password.length + 1 + host.length + 1
                + port.length + 1 + Arrays.stream(path)
                .map(pathSegment -> pathSegment.length + 1).reduce(0, Integer::sum)
                + Arrays.stream(query).map(queryParameter -> queryParameter.length + 1)
                .reduce(0, Integer::sum) + 1 + this.fragment.length + 1;
    }

    static class _UrlBuilder {

        private final static String[] EMPTY_STRING_ARRAY = new String[]{};
        final Map<String, Object> _storage = new HashMap<>();

        public _UrlBuilder withScheme(String scheme) {
            if (scheme != null) {
                this._storage.put("scheme", scheme);
            }
            return this;
        }

        public _UrlBuilder withUser(String user) {
            if (user != null) {
                this._storage.put("user", user);
            }
            return this;
        }

        public _UrlBuilder withPassword(String password) {
            if (password != null) {
                this._storage.put("password", password);
            }
            return this;
        }

        public _UrlBuilder withHost(String host) {
            if (host != null) {
                this._storage.put("host", host);
            }
            return this;
        }

        public _UrlBuilder withPort(String port) {
            if (port != null) {
                this._storage.put("port", port);
            }
            return this;
        }

        public _UrlBuilder withRootPath() {
            this._storage.put("isRootPath", true);
            return this;
        }

        public _UrlBuilder withPath(String path) {
            String[] pathSegments = path != null ? path.split("/") : null;
            if (pathSegments != null) {
                this._storage.put("path",
                        pathSegments.length > 0 && pathSegments[0].isEmpty() ? Arrays.copyOfRange(
                                pathSegments, 1, pathSegments.length) : pathSegments);
            }
            return this;
        }

        public _UrlBuilder withQuery(String query) {
            if (query != null) {
                this._storage.put("query", query != null ? query.split("&") : null);
            }
            return this;
        }

        public _UrlBuilder withFragment(String fragment) {
            if (fragment != null) {
                this._storage.put("fragment", fragment);
            }
            return this;
        }

        public _Url build() {
            return new _Url(
                    ((String) this._storage.getOrDefault("scheme", "")).toCharArray(),
                    ((String) this._storage.getOrDefault("user", "")).toCharArray(),
                    ((String) this._storage.getOrDefault("password", "")).toCharArray(),
                    ((String) this._storage.getOrDefault("host", "")).toCharArray(),
                    ((String) this._storage.getOrDefault("port", "")).toCharArray(),
                    (Boolean) this._storage.getOrDefault("isRootPath", false),
                    Arrays.stream(
                                    ((String[]) this._storage.getOrDefault("path", EMPTY_STRING_ARRAY)))
                            .map(
                                    String::toCharArray).collect(
                                    Collectors.toList()).toArray(new char[][]{}),
                    Arrays.stream(
                                    ((String[]) this._storage.getOrDefault("query", EMPTY_STRING_ARRAY)))
                            .map(
                                    String::toCharArray).collect(
                                    Collectors.toList()).toArray(new char[][]{}),
                    ((String) this._storage.getOrDefault("fragment", "")).toCharArray()
            );
        }
    }

}

public final class U {
    // scheme:[//[user:password@]host[:port]][/]path[?query][#fragment]

    private _Url _storage;

    private U() {
    }

    public static U from(URL url) {
        U result = new U();
        _Url._UrlBuilder urlBuilder = new _Url._UrlBuilder();
        urlBuilder.withScheme(url.getProtocol());
        if (url.getUserInfo() != null) {
            String[] userInfo = url.getUserInfo().split(":");
            urlBuilder.withUser(userInfo[0]);
            if (userInfo.length > 1) {
                urlBuilder.withPassword(userInfo[1]);
            }
        }
        urlBuilder.withPort(url.getPort() >= 0 ? "" + url.getPort() : null);
        urlBuilder.withHost(url.getHost());
        String urlPath = url.getPath();
        if (urlPath.length() > 0) {
            urlBuilder.withRootPath();
        }
        urlBuilder.withPath(urlPath);
        urlBuilder.withQuery(url.getQuery());
        urlBuilder.withFragment(url.getRef());

        result._storage = urlBuilder.build();

        return result;
    }

    public U replacePathSegment(int index, String segment) {
        if (segment.indexOf('/') > -1) {
            throw new IllegalStateException("Unimplemented");
        }
        if (this._storage != null && Math.abs(index) < this._storage.path.length && index >= 0) {
            this._storage.path[index] = segment.toCharArray();
        }
        if (this._storage != null && Math.abs(index) < this._storage.path.length && index < 0) {
            this._storage.path[this._storage.path.length - Math.abs(index)] = segment.toCharArray();
        }
        return this;
    }

    public URL toUrl() throws MalformedURLException {
        if (this._storage == null) {
            return null;
        }
        return new URL(this._storage.toString());
    }


}
