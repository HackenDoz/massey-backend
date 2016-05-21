import json.JSONObject;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

public class User implements Serializable {

	public final int id;
	public final String username;
	public final String name;
    public final String email;
	public byte[] password;
	public byte[] salt;

	public HashMap<Integer, Hackathon> joinedHackathons = new HashMap<>();

	public User(int id, String username, String name, String email) {
		this.id = id;
		this.username = username;
		this.name = name;
        this.email = email;
	}

	public void setPassword(byte[] pass) {
		SecureRandom random = new SecureRandom();
		salt = new byte[64];
		random.nextBytes(salt);
		byte[] toHash = new byte[pass.length + salt.length];
		System.arraycopy(pass, 0, toHash, 0, pass.length);
		System.arraycopy(salt, 0, toHash, pass.length, salt.length);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			password = digest.digest(toHash);
		} catch(NoSuchAlgorithmException e) {
			System.out.println("Sha-256 failed");
			e.printStackTrace();
		}
	}

	public boolean verifyPassword(byte[] pass) {
		byte[] toHash = new byte[pass.length + salt.length];
		System.arraycopy(pass, 0, toHash, 0, pass.length);
		System.arraycopy(salt, 0, toHash, pass.length, salt.length);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] result = digest.digest(toHash);
			if(result.length != password.length) {
				return false;
			}
			for(int i = 0; i < result.length; ++i) {
				if(result[i] != password[i])
					return false;
			}
			return true;
		} catch (NoSuchAlgorithmException e) {
			System.out.println("SHA-256 failed");
			e.printStackTrace();
		}

		return false;

	}


}
