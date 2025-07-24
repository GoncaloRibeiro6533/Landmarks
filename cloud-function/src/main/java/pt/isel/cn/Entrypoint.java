package pt.isel.cn;

import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;

import java.io.BufferedWriter;


public class Entrypoint implements HttpFunction {

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        BufferedWriter writer = response.getWriter();
        String instanceGroup = request.getFirstQueryParameter("name").orElseThrow();
        String zone = "europe-southwest1-b";
        String projectId = "cn2425-t1-g09";
        IPList ipList = new IPList();
        try (InstancesClient client = InstancesClient.create()) {
            for (Instance instance : client.list(projectId, zone).iterateAll()) {
                if ( instance.getStatus().compareTo("RUNNING") ==0 &&
                        instance.getName().contains(instanceGroup)) {
                    String ip = instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                    ipList.ips.add(ip);
                }
            }
        }
        response.setContentType("application/json");
        writer.write(new Gson().toJson(ipList));
    }
}


