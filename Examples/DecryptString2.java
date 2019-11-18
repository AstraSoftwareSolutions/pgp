import com.didisoft.pgp.PGPLib;
 
public class DecryptString2 {
    public static void main(String[] args) throws Exception {
        // create an instance of the library
        PGPLib pgp = new PGPLib();
 
        String openpgpMessage = pgp.encryptString("Hello World", "c:\\Projects\\PGPKeys\\public.key");
 
        String privateKeyFile = "c:\\Projects\\PGPKeys\\private.key";
        String privateKeyPassword = "changeit";
 
 		String decryptedMessage = pgp.decryptString(openpgpMessage,
                                                    	privateKeyFile,
                                                    	privateKeyPassword);
        System.out.println("plain message: " + decryptedMessage);
    }
}