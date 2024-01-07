package com.leitianyu.blue.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * 通过basePackage扫描目录下所有文件并筛选
 *
 * @author leitianyu
 * @date 2024/1/7
 */
public class ResourceResolver {

    Logger logger = LoggerFactory.getLogger(getClass());

    //扫描路径
    String basePackage;


    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }


    public <R> List<R> scan(Function<Resource, R> mapper) {
        //符号替换
        String basePackagePath = this.basePackage.replace(".", "/");
        try {
            List<R> collector = new ArrayList<>();
            scan0(basePackagePath, collector, mapper);
            return collector;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    <R> void scan0(String basePackagePath, List<R> collector, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        logger.atDebug().log("scan path: {}", basePackagePath);
        //这玩意源码真复杂
        //获取绝对路径
        Enumeration<URL> enumeration = getContextClassLoader().getResources(basePackagePath);
        while (enumeration.hasMoreElements()) {
            //获取项目绝对路径
            URL url = enumeration.nextElement();
            URI uri = url.toURI();
            String uriStr = removeTrailingSlash(uriToString(uri));
            String uriBaseStr = uriStr.substring(0, uriStr.length() - basePackagePath.length());
            if (uriBaseStr.startsWith("file:")) {
                uriBaseStr = uriBaseStr.substring(5);
            }
            if (uriStr.startsWith("jar:")) {
                scanFile(true, uriBaseStr, jarUriToPath(basePackagePath, uri), collector, mapper);
            } else {
                scanFile(false, uriBaseStr, Paths.get(uri), collector, mapper);
            }
        }
    }

    /**
     * 扫描文件
     */
    <R> void scanFile(boolean isJar, String base, Path root, List<R> collector, Function<Resource, R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(base);
        //遍历路径下的文件并将信息封装进Resource存入collector
        Files.walk(root).filter(Files::isRegularFile).forEach(file -> {
            Resource res;
            if (isJar) {
                res = new Resource(baseDir, removeLeadingSlash(file.toString()));
            } else {
                String path = file.toString();
                String name = removeLeadingSlash(path.substring(baseDir.length()));
                res = new Resource("file:" + path, name);
            }
            logger.atDebug().log("found resource: {}", res);
            //进一步筛选文件
            R r = mapper.apply(res);
            if (r != null) {
                collector.add(r);
            }
        });
    }

    //去头
    String removeLeadingSlash(String s) {
        if (s.startsWith("/") || s.startsWith("\\")) {
            s = s.substring(1);
        }
        return s;
    }


    Path jarUriToPath(String basePackagePath, URI jarUri) throws IOException {
        return FileSystems.newFileSystem(jarUri, new HashMap<>()).getPath(basePackagePath);
    }

    //去尾
    String removeTrailingSlash(String s) {
        if (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }


    String uriToString(URI uri) throws UnsupportedEncodingException {
        return URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8.toString());
    }

    //获取类加载器
    ClassLoader getContextClassLoader() {
        ClassLoader cl = null;
        cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }

}
