package net.simplyrin.kokuminipchecker;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.config.Configuration;
import net.simplyrin.config.Config;
import net.simplyrin.kokuminipchecker.servlet.RequestTask;
import net.simplyrin.rinstream.RinStream;

/**
 * Created by SimplyRin on 2021/01/17.
 *
 * Copyright (c) 2021 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation dataFiles (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@Getter
public class Main {

	public static void main(String[] args) {
		new Main().run();
	}

	private Gson gson = new Gson();

	private Configuration config;
	private Configuration data;

	private Thread commandThread;
	private ExecutorService rateService = Executors.newFixedThreadPool(40);
	private ExecutorService fetchService = Executors.newFixedThreadPool(128);

	private List<String> queued = new ArrayList<>();

	public void run() {
		new RinStream();

		System.out.println("準備中...");

		File file = new File("config.yaml");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Configuration config = Config.getConfig(file);
			config.set("Cache", 14);
			Config.saveConfig(config, file);
		}
		this.config = Config.getConfig(file);


		File dataFile = new File("data.yaml");
		if (!dataFile.exists()) {
			try {
				dataFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.data = Config.getConfig(dataFile);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				Config.saveConfig(data, dataFile);
			}
		});

		System.out.println("サーバーの準備をしています...");

		ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);

		servletHandler.setMaxFormContentSize(1024 * 1024 * 1024);
		servletHandler.addServlet(new ServletHolder(new RequestTask(this)), "/request");

		HandlerList handlerList = new HandlerList();
		handlerList.addHandler(servletHandler);

		Server server = new Server();
		server.setHandler(handlerList);

		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);

		HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfiguration);
		ServerConnector serverConnector = new ServerConnector(server, httpConnectionFactory);
		serverConnector.setPort(53185);
		server.setConnectors(new Connector[] { serverConnector });

		try {
			System.out.println("/request でリクエストの待受を開始します。");
			server.start();
			server.join();
			System.out.println("/request でリクエストの待受を開始しました。");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * ip-api
	 */
	public RequestData get(String ip) {
		if (ip.startsWith("127.0")
				|| ip.startsWith("192.168.")
				|| ip.startsWith("10.")
				|| ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.") || ip.startsWith("172.19.")
				|| ip.startsWith("172.2")
				|| ip.startsWith("172.30.") || ip.startsWith("172.31.")
				|| ip.startsWith("localhost")
				|| ip.startsWith("MSI")
				|| ip.startsWith("DESKTOP-")
				|| ip.startsWith("LAPTOP-")) {
			return new RequestData(null, false);
		}

		if (this.data != null && this.data.getString(ip + ".JSON", null) != null) {
			long expires = this.data.getLong(ip + ".EXPIRES");
			long now = new Date().getTime();

			// 有効期限が失効していない場合
			if (expires >= now) {
				JsonElement json = JsonParser.parseString(this.data.getString(ip + ".JSON"));
				return new RequestData(this.gson.fromJson(json, IpData.class), true);
			} else {
				System.out.println("[CACHE EXPIRES] " + ip);
			}
		}
		if (this.queued.contains(ip)) {
			System.out.println("[READY] Query: " + ip);
			return new RequestData(null, false);
		}
		this.queued.add(ip);
		this.rateService.execute(() -> {
			Random rand = new Random();
		    int first = rand.nextInt(15);
		    int end = 60 - first;
		    try {
		    	System.out.println("[SLEEP] " + first + "s, Query: " + ip);
		    	TimeUnit.SECONDS.sleep(first);
		    } catch (Exception e) {
		    }

			this.fetchService.execute(() -> {
				try {
					System.out.println("[GET] Query: " + ip);
					HttpURLConnection connection = (HttpURLConnection) new URL("http://ip-api.com/json/" + ip + "?fields=66846719").openConnection();
					String result = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
					JsonElement json = JsonParser.parseString(result);
					JsonObject jsonObject = json.getAsJsonObject();

					if (jsonObject.has("status") && jsonObject.get("status").getAsString().equals("success")) {
						this.data.set(ip + ".JSON", json.toString());

						// キャッシュ設定
						Calendar calendar = Calendar.getInstance();
						calendar.add(Calendar.DATE, this.config.getInt("Cache", 14));
						this.data.set(ip + ".EXPIRES", calendar.getTime().getTime());

						IpData data = this.gson.fromJson(json, IpData.class);
						System.out.println("[DONE] Query: " + ip
								+ ", isMobile: " + data.getMobile()
								+ ", isProxy: " + data.getProxy()
								+ ", isHosting: " + data.getHosting());

						Config.saveConfig(this.data, new File("data.yaml"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			this.queued.remove(ip);

			try {
				TimeUnit.SECONDS.sleep(end);
			} catch (Exception e) {
			}
		});
		return new RequestData(null, false);
	}

	@Getter @AllArgsConstructor
	public class RequestData {
		private IpData ipData;
		private boolean cached;
	}

}
