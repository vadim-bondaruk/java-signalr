// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

import com.google.gson.Gson;
import com.microsoft.aspnet.signalr.*;
import org.apache.commons.io.IOUtils;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;

public class CovrSignalRClient {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public static void main(String[] args) throws Exception {

        System.out.println("Enter URL:");
        Scanner reader = new Scanner(System.in);
        String url, client, secret;

        url = reader.nextLine();
        System.out.println("Enter client identifier:");
        client = reader.nextLine();
        System.out.println("Enter client secret:");
        secret = reader.nextLine();

        String hubUrl = String.format("%s/userNotification",url);

        String json = String.format("{\"id\":\"%s\", \"secret\":\"%s\"}", client, secret);
        URL authUrl = new URL(String.format("%s/api/auth",url));
        HttpsURLConnection connection = (HttpsURLConnection)authUrl.openConnection();
        connection.setConnectTimeout(5000);
        connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        OutputStream output = connection.getOutputStream();
        output.write(json.getBytes("UTF-8"));
        output.close();
        InputStream in = new BufferedInputStream(connection.getInputStream());
        String result = IOUtils.toString(in,"UTF-8");
        System.out.println(result);
        Gson gson = new Gson();
        TokenResponse tokenResponse = gson.fromJson(result,TokenResponse.class);
        System.out.println(tokenResponse.accessToken);

        String temp = hubUrl+"?token="+tokenResponse.accessToken;

        HashMap<String, String> headers = new HashMap<>();

        headers.put("Authorization", "Bearer "+tokenResponse.accessToken);

        Transport transport = new WebSocketTransport(hubUrl, new NullLogger(), headers);

        int count = 0;

        while (count < 10)
        {
            try
            {
                count++;
                HubConnection hubConnection = new HubConnectionBuilder()
                        .withUrl(temp,transport)
                        .configureLogging(LogLevel.Information)
                        .build();
                hubConnection.start();
                System.out.println("Connection State: "+ hubConnection.getConnectionState());

                hubConnection.on("ConsumerGet",(data)-> System.out.println("Consumer get invoked, response: "+data),String.class);
                hubConnection.send("ConsumerGet");
                Thread.sleep(5000);
                hubConnection.stop();
                System.out.println("Disconnected");
                System.out.println(ANSI_GREEN+"SUCCESS"+ANSI_RESET);
            }
            catch (Exception ex){
                System.out.println(ex);
                System.out.println(ANSI_RED+"ERROR"+ANSI_RESET);
            }
        }
    }
}

