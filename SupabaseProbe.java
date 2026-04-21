import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SupabaseProbe {
  public static void main(String[] args) throws Exception {
    var client = HttpClient.newBuilder().build();
    var req = HttpRequest.newBuilder()
      .uri(URI.create("https://dtmbbanzhmqxgapfbcfi.supabase.co/auth/v1/health"))
      .GET()
      .build();
    var resp = client.send(req, HttpResponse.BodyHandlers.ofString());
    System.out.println("STATUS=" + resp.statusCode());
    System.out.println(resp.body());
  }
}
