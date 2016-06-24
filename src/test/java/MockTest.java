import com.example.helloworld.resources.HelloWorldResource;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Created by vagrant on 6/24/16.
 */
public class MockTest {

    JwtConsumer consumer;
    protected void getPublicKey() {
        FileInputStream is = null;
        try {
            is = new FileInputStream("/etc/ssl/certs/dw-bnr-auth-0.cloudapp.net.cert.pem");
        } catch (FileNotFoundException ex) {
        }

        PublicKey key = null;
        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509"); //returns a certificate factory object that implements X.509
            X509Certificate cer = (X509Certificate) fact.generateCertificate(is); //initializes certificate with contents from input stream
            key = cer.getPublicKey();
        } catch (CertificateException ex) {
        }

        consumer = new JwtConsumerBuilder()
                .setAllowedClockSkewInSeconds(30) // allow some leeway in validating time based claims to account for clock skew
                .setRequireExpirationTime() // the JWT must have an expiration time
                .setRequireSubject() // the JWT must have a subject claim
                .setVerificationKey(key) // verify the signature with the public key
                .setRelaxVerificationKeyValidation() // relaxes key length requirement
                .build();

    }

    @Rule
    public ResourceTestRule rule = ResourceTestRule
            .builder()
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(new AuthDynamicFeature(new JwtAuthFilter.Builder<User>()
                    .setJwtConsumer(consumer)
                    .setAuthenticator(new JwtAuthenticator())
                    .setAuthorizer(new JwtAuthorizer())
                    .setRealm("realm")
                    .setPrefix("Bearer")
                    .buildAuthFilter()))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(User.class))
            .addResource(new HelloWorldResource("Hello, %s!", "Stranger"))
            .build();

    String token = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJmcmFuY2VzLnRzbyIsImV4cCI6MTQ2NjgxMTM4M30.YLX3uLrFVS4TxwSEnhOZhBl2K2GQgxxJimzIQ-T6IE3l4REi9TsdI4eQZXcPhqqHw5ZLGCpqYKrUkNCmk63vurPG2bQaSEZSxp6DqQWcn6--2HUfgGcWAF2UA7GrfqQZj7DcfYCfi8FSHDrWD4wvCWi2wHfBM-dEMCVerYuQT7YdRtilu3TDCm19ULiCbLH7APLV9-C80BzW2TeO1o7ZYHxHPXTICWrN2ZeQWyD1j9FRf31YngxEsflF8UyfifjB-Wk9cOZ7caAMIP9eBkzm5XVyNu_1VzJuh9-dMhw_o2ukDhgQ79jRLFUkOjDdT96Pj89aHTZh4CeHGv0PRgDYXg";

    @Test
    public void test() throws Exception{
        final Response response = rule.getJerseyTest().target("/hello-world")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", "Bearer " + token)
                .get();
    }
}
