package io.github.griffenx.CityZen;

import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class Citizen {
	private ConfigurationSection properties;
	
	public Citizen(UUID uuid) {
		this((Player) CityZen.getPlugin().getServer().getOfflinePlayer(uuid));
	}
	public Citizen(Player player) {
		properties = CityZen.citizenConfig.getConfig().getConfigurationSection("citizens." + player.getUniqueId().toString());
		
		//TODO: write setter methods for passport and alerts
		//passport = player;
		
		//alerts = properties.getStringList("alerts");
	}
	
	/**
	 * Gets a Citizen by name from list of citizens in memory. 
	 * If somehow that player is online, but their citizen record is not in memory, it will be added.
	 * This method should be used exclusively for getting online citizens, NOT initializing a new Citizen.
	 * @param uuid
	 * The uuid of the player to get
	 * @return
	 * Returns a citizen from memory if online, or a new citizen record if offline.
	 */
	public static Citizen getCitizen(UUID uuid) {
		Citizen ctz = null;
		for(Citizen c : CityZen.citizens) {
			if (c.getPassport().getUniqueId().equals(uuid)) {
				ctz = c;
			}
		}
		if (ctz == null) {
			Player newCtz = CityZen.getPlugin().getServer().getPlayer(uuid);
			ctz = new Citizen(newCtz);
			if (newCtz.isOnline()) CityZen.citizens.add(ctz);
		}
		return ctz;
	}
	
	public int getReputation() {
		int rep;
		try {
			rep = Integer.valueOf(getProperty("reputation"));
		} catch (NumberFormatException e) {
			rep = -1;
		}
		return rep;
	}
	
	public void addReputation(int amount) {
		int rep = getReputation() + amount;
		fixRep();
		setProperty("reputation",rep);
	}
	
	public void subReputation(int amount) {
		int rep = getReputation() - amount;
		fixRep();
		setProperty("reputation",rep);
	}
	
	public void setReputation(int amount) {
		if (amount >= 0 && amount < Integer.MAX_VALUE) setProperty("reputation",amount);
	}
	
	public City getAffiliation() {
		City aff = null;
		String affname = getProperty("affiliation");
		if (affname.length() > 0) {
			aff = new City(affname);
		}
		return aff;
	}
	
	/**
	 * Used to set which city this Citizen is affiliated with. DO NOT USE to add a player to a city.
	 * You should instead call addCitizen() on the city.
	 * @param city
	 * The City to set as this player's affiliation
	 */
	public void setAffiliation(City city) {
		if (city != null) setProperty("affiliation",city.identifier);
		else setProperty("affiliation",null);
	}
	
	public Player getPassport() {
		String foundUUID = null;
		Player passport = null;
		ConfigurationSection cits = CityZen.citizenConfig.getConfig().getConfigurationSection("citizens");
		for (String key : cits.getKeys(false)) {
			if (cits.getString(key + ".name").equalsIgnoreCase(CityZen.getPlugin().getServer().getOfflinePlayer(UUID.fromString(key)).getName())) {
				foundUUID = key;
				break;
			}
		}
		if (foundUUID != null) {
			passport = (Player) CityZen.getPlugin().getServer().getOfflinePlayer(UUID.fromString(foundUUID));
		}
		return passport;
	}
	
	public List<String> getAlerts() {
		return CityZen.citizenConfig.getConfig().getStringList("citizens." + getPassport().getUniqueId().toString() + ".alerts");
	}
	
	public void addAlert(String alertText) {
		//TODO: Date of today + alertText, then write back to citizen file
		// SimpleDateFormat?
		//alerts.add( + alertText);
	}
	
	/**
	 * Determines if two Citizen objects are the same by their UUID
	 * @param citizen
	 * The citizen to compare to this one
	 * @return
	 * Whether or not these Citizens are the same
	 */
	public Boolean equals(Citizen citizen) {
		return getPassport().getUniqueId().equals(citizen.getPassport().getUniqueId());
	}
	
	private void fixRep() {
		if (getReputation() < 0) setReputation(0);
	}
	
	private String getProperty(String property) {
		for (String prop : properties.getKeys(false)) {
			if (prop.equalsIgnoreCase(property)) {
				//TODO: I'm not sure this is correct
				// Check this in the city class as well
				return CityZen.citizenConfig.getConfig().getString(properties.getString(property));
				// No need to set defaults, as there are no defaults for citizens
			}
		}
		return "";
	}
	
	private void setProperty(String property, Object value) {
		for (String prop : properties.getKeys(false)) {
			if (prop.equalsIgnoreCase(property)) {
				CityZen.citizenConfig.getConfig().set("citizens." + getPassport().getUniqueId().toString() + property,value);
				CityZen.citizenConfig.save();
			}
		}
	}
}
