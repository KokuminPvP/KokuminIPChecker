package net.simplyrin.kokuminipchecker.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.simplyrin.kokuminipchecker.KokuminIPChecker;
import net.simplyrin.kokuminipchecker.KokuminIPChecker.RequestData;

/**
 * Created by SimplyRin on 2021/01/17.
 *
 * Copyright (c) 2021 SimplyRin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
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
@RequiredArgsConstructor
public class RequestTask extends HttpServlet {

	private final KokuminIPChecker instance;

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.addHeader("Access-Control-Allow-Origin", "*");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type");
		response.setContentType("application/json; charset=UTF-8");
		
		if (request.getQueryString() == null || request.getQueryString().length() <= 4) {
			return;
		}

		PrintWriter printWriter = response.getWriter();
		JsonObject jsonObject = new JsonObject();

		String query = URLDecoder.decode(request.getQueryString(), "UTF-8");
		System.out.println("[REQUEST] " + query);

		if (query.startsWith("[") && query.endsWith("]")) {
			jsonObject.addProperty("success", true);
			JsonArray data = new JsonArray();

			JsonArray jsonArray = JsonParser.parseString(query).getAsJsonArray();
			for (int i = 0; i < jsonArray.size(); i++) {
				String ipQuery = jsonArray.get(i).getAsString();

				RequestData rdata = this.instance.get(jsonArray.get(i).getAsString());
				if (!rdata.isCached()) {
					continue;
				}

				JsonObject item = new JsonObject();
				item.addProperty("query", ipQuery);
				item.addProperty("country", rdata.getIpData().getCountryCode());
				item.addProperty("region", rdata.getIpData().getRegionName());
				item.addProperty("mobile", rdata.getIpData().getMobile());
				item.addProperty("proxy", rdata.getIpData().getProxy());
				item.addProperty("hosting", rdata.getIpData().getHosting());
				data.add(item);
			}

			jsonObject.add("data", data);
		} else {
			jsonObject.addProperty("success", false);
		}

		printWriter.println(jsonObject.toString());
		printWriter.close();
	}

}
