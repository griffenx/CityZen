package io.github.griffenx.CityZen.Commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

import io.github.griffenx.CityZen.Citizen;
import io.github.griffenx.CityZen.City;
import io.github.griffenx.CityZen.CityZen;
import io.github.griffenx.CityZen.Messaging;
import io.github.griffenx.CityZen.Tasks.ClearMetadataTask;

public class CityCommand {
	private static final Plugin plugin = CityZen.getPlugin();
	
	public static boolean delegate(CommandSender sender, String[] args) {
		switch (args[0].toLowerCase()) {
			case "join":
				join(sender,args);
				break;
			case "list":
				list(sender,args);
				break;
			case "top":
				top(sender,args);
				break;
			case "create":
				create(sender,args);
				break;
			case "leave":
				leave(sender,args);
				break;
			case "info":
				info(sender,args);
				break;
			case "evict":
				evict(sender,args);
				break;
			case "dissolve":
				dissolve(sender,args);
				break;
			case "ban":
				ban(sender,args);
				break;
			case "unban":
			case "pardon":
				pardon(sender,args);
				break;
			case "accept":
				accept(sender,args);
				break;
			case "deny":
				deny(sender,args);
				break;
			case "banlist":
				banlist(sender,args);
				break;
			case "distance":
				distance(sender, args);
				break;
			case "who":
			case "whois":
			case "people":
			case "visitors":
				visitors(sender, args);
				break;
			default:
				return false;
		}
		return true;
	}
	
	private static void join(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			if (sender.hasPermission("cityzen.city.join")) {
				
				String cityName = "";
				for (int i = 1; i < args.length; i++){
					cityName += args[i];
				}
				City city = City.getCity(cityName);
				if (city != null) {
					
					Citizen citizen = Citizen.getCitizen(sender);
					if (citizen != null) {
						
						City preexistingWaitlist = citizen.getWaitlistedCity();
						if (preexistingWaitlist == null) {
							
							if (city.isFreeJoin()) {
								
								city.addCitizen(citizen);
								sender.sendMessage("You successfully joined " + cityName + "!");
								
								long rep = plugin.getConfig().getLong("gainedOnJoinCity");
								citizen.addReputation(rep);
								sender.sendMessage("You gained " + rep + " Reputation for joining!");
								
							} else {
								city.addWaitlist(citizen);
								sender.sendMessage(city.getChatName() + ChatColor.BLUE + " does not allow Free Join."
										+ " A request to join this City has been sent instead and will be reviewed by an official of that City."
										+ " To cancel this request, type \"/city cancel\"");
								
								String alertMessage = citizen.getName() + " has requested to join " + city.getName() 
									+ ". Type \"/city approve " + citizen.getName() + "\" or \"/city deny " + citizen.getName() + "\"";
								city.getMayor().addAlert(alertMessage);
								for (Citizen d : city.getDeputies()) d.addAlert(alertMessage);
							}
						} else {
							sender.sendMessage(ChatColor.RED + "You are already on the waitlist for " + preexistingWaitlist.getChatName()
								+ ChatColor.RED + ". Please cancel your request there or wait for it to be denied before attempting to join another city.");
						}
					} else {
						sender.sendMessage(Messaging.missingCitizenRecord());
					}
				} else {
					sender.sendMessage(Messaging.cityNotFound(cityName));
				}
			} else {
				sender.sendMessage(Messaging.noPerms("cityzen.city.join"));
			}
		} else {
			sender.sendMessage(Messaging.playersOnly());
		}
	}
	
	private static void list(CommandSender sender, String[] args) {
		if (sender.hasPermission("cityzen.city.list")) {
			List<City> cities = City.getCities();
			if (cities.size() > 0) {
				int pageNumber;
				try {
					pageNumber = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					pageNumber = 1;
				}
				
				// Puts together the page of cities to display
				int numberOfPages = (int) Math.ceil(cities.size() / 5);
				if (pageNumber < 1) pageNumber = 1;
				else if (pageNumber > numberOfPages) pageNumber = numberOfPages;
				
				String[][] listCities = new String[numberOfPages][5];
				for (int page = 0; page < numberOfPages; page++) {
					for (int c = 0; c < 5; c++) {
						try {
							City city = cities.get((page * 5) + c);
							listCities[page][c] = city.getChatName() + ChatColor.BLUE + city.getSlogan();
						} catch (IndexOutOfBoundsException e) {
							listCities[page][c] = null;
						}
					}
				}
				sender.sendMessage(ChatColor.RED + "Cities on " + ChatColor.GOLD + "" + ChatColor.BOLD + plugin.getServer().getServerName() 
						+ ChatColor.RESET + "" + ChatColor.RED + "(" + pageNumber + "/" + numberOfPages);
				sender.sendMessage(listCities[pageNumber - 1]);
				if (numberOfPages > 1 && pageNumber < numberOfPages) sender.sendMessage(ChatColor.BLUE + "Type \"/cities list " + (pageNumber + 1) + "\" to view the next page.");
				sender.sendMessage(ChatColor.BLUE + "See more info about a city with \"/city info <City Name...>\"");
			} else {
				sender.sendMessage(ChatColor.BLUE + "There are no Cities on this server.");
			}
		} else {
			sender.sendMessage(Messaging.noPerms("cityzen.city.list"));
		}
	}
	
	private static boolean top(CommandSender sender, String[] args) {
		if (sender.hasPermission("cityzen.city.top")) {
			int numberOfResults = 10;
			try {
				numberOfResults = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				numberOfResults = 10;
			}
			
			if (numberOfResults < 1) numberOfResults = 1;
			else if (numberOfResults > 10) numberOfResults = 10;
			List<City> cities = City.getCities();
			if (cities.size() < numberOfResults) numberOfResults = cities.size();
			
			String sortType = "Reputation";
			
			if (args[2].length() == 0 || args[2].equalsIgnoreCase("reputation")) {
				sortType = "Reputation";
			} else if (args[2].equalsIgnoreCase("citizens") || args[2].equalsIgnoreCase("population")) {
				sortType = "Population";
			} else if (args[2].equalsIgnoreCase("date") || args[2].equalsIgnoreCase("age")) {
				sortType = "Age";
			}
			
			List<Long> values = new ArrayList<Long>();
			// Sort by type
			for (City c : cities) {
				switch (sortType) {
					case "Reputation":
						values.add(c.getReputation());
						break;
					case "Population":
						values.add((long) c.getCitizens().size());
						break;
					case "Age":
						Date foundingDate = c.getFoundingDate();
						values.add(foundingDate.getTime());
						break;
				}
			}
			Collections.sort(values);
			if (!sortType.equals("Age")) Collections.reverse(values);
			
			sender.sendMessage(ChatColor.BLUE + "Top " + ChatColor.GOLD + numberOfResults + ChatColor.BLUE + " Cities by " + sortType + ":");
			for (int i = 0; i < numberOfResults; i++) {
				for (City c : cities) {
					if (values.get(i) != null && values.get(i) == c.getReputation()) {
						sender.sendMessage(ChatColor.BLUE + "| " + i + ". " + ChatColor.RED + values.get(i)
							+ ChatColor.BLUE + " - " + c.getChatName());
						cities.remove(c);
					}
				}
			}
		} else {
			sender.sendMessage(Messaging.noPerms("cityzen.city.top"));
		}
		return true;
	}
	
	private static void create(CommandSender sender, String[] args) {
		if (args.length > 1) {
			if (sender.hasPermission("cityzen.city.create")) {
				if (Character.isAlphabetic(args[1].charAt(0))) {
					City newCity;
					String cityName = "";
					for (int i = 1; i < args.length; i++) {
						cityName += args[i];
					}
					if (sender instanceof Player) {
						Citizen creator = Citizen.getCitizen(sender);
						if (creator != null) {
							if (creator.getAffiliation() == null && !creator.isWaitlisted()) {
								newCity = City.createCity(cityName,creator);
								if (newCity != null) creator.addReputation(CityZen.getPlugin().getConfig().getLong("reputation.gainedOnCreateCity"));
							} else {
								sender.sendMessage(ChatColor.RED + "You cannot create a City if you are already a Citizen of a City, or are on the Waitlist for a City.");
								return;
							}
						} else {
							sender.sendMessage(Messaging.missingCitizenRecord());
							return;
						}
					} else newCity = City.createCity(cityName);
					if (newCity != null) sender.sendMessage(ChatColor.BLUE + "Congratulations! You founded " + ChatColor.GOLD + cityName);
					else sender.sendMessage(ChatColor.RED + "A city already exists by the name " + ChatColor.GOLD + cityName + ChatColor.RED + ". Please try again with a unique name.");
				} else sender.sendMessage(ChatColor.RED + "City names must start with a letter. Please use a valid City name."); 
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.create"));
		} else sender.sendMessage(ChatColor.RED + "Not enough arguments. Usage: \"/city create <City Name...>\"");
	}
	
	private static void leave(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Citizen citizen = Citizen.getCitizen(sender);
			if (sender.hasPermission("cityzen.city.leave")) {
				City aff = citizen.getAffiliation();
				if (aff != null) {
					long rep = citizen.getReputation();
					aff.removeCitizen(citizen);
					sender.sendMessage(ChatColor.BLUE + "You have left " + ChatColor.GOLD + aff.getChatName() + ChatColor.BLUE + ". "
						+ (CityZen.getPlugin().getConfig().getInt("lostOnLeaveCityPercent") > 0 ? " You lost " + ChatColor.GOLD + (rep - citizen.getReputation())
						+ " Reputation" + ChatColor.BLUE + ". You now have " + ChatColor.GOLD + citizen.getReputation() + " Reputation" + ChatColor.BLUE + "." : ""));
				} else sender.sendMessage(ChatColor.RED + "You have no city to leave.");
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.leave"));
		} else sender.sendMessage(Messaging.playersOnly());
	}
	
	private static void info(CommandSender sender, String[] args) {
		if (sender.hasPermission("cityzen.city.info")) {
			City city;
			if (sender instanceof Player && args.length == 1) {
				Citizen citizen = Citizen.getCitizen(sender);
				city = citizen.getAffiliation();
				if (city == null) {
					sender.sendMessage(ChatColor.RED + "You are not a Citizen of any City."
						+ " Please specify a City to look up. Useage: \"/city info <City>\"");
					return;
				}
			} else if (args.length > 1) {
				city = City.getCity(args[1]);
			} else {
				sender.sendMessage(ChatColor.RED + "You must specify a City in order to run this command from Console."
					+ "Useage:\"/city info <City>\"");
				return;
			}
			
			int deps = city.getDeputies().size();
			String[] messages = {
				ChatColor.BLUE + "==============================",
				ChatColor.GOLD + "     " + city.getChatName(),
				ChatColor.BLUE + "| Slogan: " + ChatColor.WHITE + "\"" + city.getSlogan() + "\"",
				ChatColor.BLUE + "| Date Founded: " + ChatColor.WHITE + city.getFoundingDate("dd MMM yyyy"),
				ChatColor.BLUE + "| Population: " + ChatColor.WHITE + city.getCitizens().size(),
				ChatColor.BLUE + "| Reputation: " + ChatColor.GOLD + city.getReputation(),
				ChatColor.BLUE + "| Mayor: " + ChatColor.GOLD + city.getMayor().getName() 
					+ (deps > 0 ? "(" + deps + " Deput" + (deps > 1 ? "ies" : "y") + ")" : ""),
				ChatColor.BLUE + "| Plots: " + ChatColor.WHITE + city.getPlots().size(),
				ChatColor.BLUE + "| FreeJoin: " + city.isFreeJoin() + " OpenPlotting: " + city.isOpenPlotting() + " BlockExclusion: " 
					+ (city.isBlockExclusion() ? (city.isWhitelisted() ? "Whitelist" : "Blacklist") : "None")
			};
			
			sender.sendMessage(messages);
		} else sender.sendMessage(Messaging.noPerms("cityzen.city.info"));
	}
	
	private static void evict(CommandSender sender, String[] args) {
		Citizen citizen = null;
		if (sender instanceof Player) citizen = Citizen.getCitizen(sender);
		if (sender.hasPermission("cityzen.city.evict") || (citizen != null && (citizen.isMayor() || citizen.isDeputy()))) {
			if (args.length > 1) {
				Citizen target = Citizen.getCitizen(args[1]);
				if (target != null) {
					if (citizen != null && (citizen.isMayor() || citizen.isDeputy())) {
						if (!target.getAffiliation().equals(citizen.getAffiliation())) {
							sender.sendMessage(ChatColor.RED + "You can only evict players from your City.");
							return;
						}
					}
					City city = target.getAffiliation();
					if (city != null) {
						long rep = target.getReputation();
						city.removeCitizen(target,true);
						if (target.getPassport().isOnline()) {
							String message = ChatColor.RED + "You were evicted from " + city.getChatName() + ChatColor.RED + " by " 
								+ sender.getName() + ". You lost " + ChatColor.GOLD + (rep - target.getReputation()) 
								+ ChatColor.RED + " Reputation. You have been removed from ownership of all plots"
								+ " and may not rejoin this City unless approved by a City official.";
							target.getPassport().sendMessage(message);
						} else {
							target.addAlert("[CityZen] You were evicted from " + city.getName() + " by " + sender.getName() + ". You lost "
								+ (rep - target.getReputation()) + " Reputation. You have been removed from ownership of all plots"
								+ " and may not rejoin this City unless approved by a City official.");
						}
						sender.sendMessage(ChatColor.GOLD + target.getName() + ChatColor.BLUE + " was evicted from " + city.getChatName());
					} else sender.sendMessage(Messaging.noAffiliation(target));
				} else sender.sendMessage(Messaging.citizenNotFound(args[1]));
			} else sender.sendMessage(ChatColor.RED + "Not enough arguments. Please specify a Citizen to evict from their City.");
		} else sender.sendMessage(ChatColor.RED + "You must either be a City official or have permission node cityzen.city.evict to run this command.");
	}
	
	private static void dissolve(CommandSender sender, String[] args) {
		if (args.length == 1) {
			if (sender.hasPermission("cityzen.city.dissolve")) {
				if (sender instanceof Player) {
					Citizen citizen = Citizen.getCitizen(sender);
					if (citizen != null) {
						if (citizen.isMayor()) {
							if (citizen.getPassport().hasMetadata("deleteConfirm")) {
								City city = citizen.getAffiliation();
								citizen.getPassport().removeMetadata("deleteConfirm", CityZen.getPlugin());
								List<Citizen> refugees = city.getCitizens();
								citizen.getAffiliation().delete();
								for (Citizen r : refugees) {
									if (r.getPassport().isOnline()) r.getPassport().sendMessage(ChatColor.RED + "Your city has been deleted."
										+ " You have not lost any reputation from this and are free to join another city.");
									else r.addAlert("Your city has been deleted."
										+ " You have not lost any reputation from this and are free to join another city.");
								}
								sender.sendMessage(ChatColor.BLUE + "Your city has been completely deleted!");
							} else {
								sender.sendMessage(ChatColor.RED + "Are you sure you want to delete " + citizen.getAffiliation().getChatName()
										+ ChatColor.RED + "? It will be gone forever (a long time). This action cannot be reversed. Type the command"
												+ " again in the next 60 seconds to confirm.");
								citizen.getPassport().setMetadata("deleteConfirm", new FixedMetadataValue(CityZen.getPlugin(),"asked"));
								new ClearMetadataTask((Metadatable) citizen.getPassport(),"deleteConfirm").runTaskLater(CityZen.getPlugin(), 20 * 60);
							}
						} else sender.sendMessage(Messaging.notMayor());
					} else sender.sendMessage(Messaging.missingCitizenRecord());
				} else sender.sendMessage(ChatColor.RED + "To use this command as Console or Command block, please specify a City to dissolve.");
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.dissolve"));
		} else {
			if (sender.hasPermission("cityzen.city.dissolve.others")) {
				City city = City.getCity(args[1]);
				if (city != null) {
					List<Citizen> refugees = city.getCitizens();
					sender.sendMessage(ChatColor.BLUE + "You deleted the City " + city.getChatName());
					city.delete();
					for (Citizen r : refugees) {
						if (r.getPassport().isOnline()) r.getPassport().sendMessage(ChatColor.RED + "Your city has been deleted."
							+ " You have not lost any reputation from this and are free to join another city.");
						else r.addAlert("Your city has been deleted."
							+ " You have not lost any reputation from this and are free to join another city.");
					}
				} else sender.sendMessage(Messaging.cityNotFound(args[1]));
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.dissolve.others"));
		}
	}
	
	private static void ban(CommandSender sender, String[] args) {
		City city = null;
		if (args.length == 2) {
			if (sender.hasPermission("cityzen.city.ban")) {
				if (sender instanceof Player) {
					Citizen citizen = Citizen.getCitizen(sender);
					if (citizen != null) {
						city = citizen.getAffiliation();
						if (city == null) {
							sender.sendMessage(Messaging.noAffiliation());
							return;
						}
						if (!(citizen.isMayor() || citizen.isDeputy())) {
							sender.sendMessage(Messaging.notCityOfficial());
							return;
						}
					}
				} else {
					sender.sendMessage(ChatColor.RED + "You have to specify a city to use this command"
							+ " from console or a command block. Useage:\"/city ban <player> (city)");
					return;
				}
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.ban"));
		} else {
			if (sender.hasPermission("cityzen.city.ban.others")) {
				city = City.getCity(args[2]);
				
				if (city == null) {
					sender.sendMessage(Messaging.cityNotFound(args[2]));
					return;
				}
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.ban.others"));
		}
		if (city != null) {
			Citizen target = Citizen.getCitizen(args[1]);
			if (target != null) {
				if (!city.isBanned(target)) {
					city.ban(target);
					sender.sendMessage(ChatColor.GOLD + target.getName() + ChatColor.BLUE + " has been banned from " + city.getChatName());
				} else sender.sendMessage(ChatColor.RED + "That player is already banned from " + city.getChatName());
			} else sender.sendMessage(Messaging.citizenNotFound(args[1]));
		}
	}
	
	private static void pardon(CommandSender sender, String[] args) {
		City city = null;
		if (args.length == 2) {
			if (sender.hasPermission("cityzen.city.pardon")) {
				if (sender instanceof Player) {
					Citizen citizen = Citizen.getCitizen(sender);
					if (citizen != null) {
						city = citizen.getAffiliation();
						if (city == null) {
							sender.sendMessage(Messaging.noAffiliation());
							return;
						}
						if (!(citizen.isMayor() || citizen.isDeputy())) {
							sender.sendMessage(Messaging.notCityOfficial());
							return;
						}
					}
				} else {
					sender.sendMessage(ChatColor.RED + "You have to specify a city to use this command"
							+ " from console or a command block. Useage:\"/city ban <player> (city)");
					return;
				}
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.pardon"));
		} else {
			if (sender.hasPermission("cityzen.city.pardon.others")) {
				city = City.getCity(args[2]);
				
				if (city == null) {
					sender.sendMessage(Messaging.cityNotFound(args[2]));
					return;
				}
			} else sender.sendMessage(Messaging.noPerms("cityzen.city.pardon.others"));
		}
		if (city != null) {
			Citizen target = Citizen.getCitizen(args[1]);
			if (target != null) {
				if (city.isBanned(target)) {
					city.pardon(target);
					sender.sendMessage(ChatColor.GOLD + target.getName() + ChatColor.BLUE + " has been pardoned from " + city.getChatName());
				} else sender.sendMessage(ChatColor.RED + "That player is not banned from " + city.getChatName());
			} else sender.sendMessage(Messaging.citizenNotFound(args[1]));
		}
	}
	
	private static void accept(CommandSender sender, String[] args) {
		if (sender.hasPermission("cityzen.city.accept")) {
			if (sender instanceof Player) {
				Citizen citizen = Citizen.getCitizen(sender);
				if (citizen != null) {
					City city = citizen.getAffiliation();
					if (city != null) {
						if (citizen.isMayor() || citizen.isDeputy()) {
							if (args.length > 1) {
								Citizen target = Citizen.getCitizen(args[2]);
								if (target != null) {
									if (city.isInWaitlist(target)) {
										long rep = target.getReputation();
										city.addCitizen(target);
										city.removeWaitlist(target);
										sender.sendMessage(ChatColor.GOLD + target.getName() + ChatColor.BLUE + " is now a Citizen of " + city.getChatName());
										target.sendMessage(ChatColor.GOLD + "Congratulations!" + ChatColor.BLUE + " Your request to join "
												+ city.getChatName() + ChatColor.BLUE + " has been accepted. You are now an official Citizen"
												+ " of this City." + (target.getReputation() > rep ? " You gained " + ChatColor.GOLD 
												+ (target.getReputation() - rep) + " Reputation." : ""));
									} else sender.sendMessage(ChatColor.RED + target.getName() + " is not on the waitlist for " + city.getChatName());
								} else sender.sendMessage(Messaging.citizenNotFound(args[2]));
							} else sender.sendMessage(ChatColor.RED + "Not enough arguments. Please specify a player whose join request to accept.");
						} else sender.sendMessage(Messaging.notCityOfficial());
					} else sender.sendMessage(Messaging.noAffiliation());
				} else sender.sendMessage(Messaging.missingCitizenRecord());
			} else sender.sendMessage(Messaging.playersOnly());
		} else sender.sendMessage(Messaging.noPerms("cityzen.city.accept"));
	}
	
	private static void deny(CommandSender sender, String[] args) {
		if (sender.hasPermission("cityzen.city.accept")) {
			if (sender instanceof Player) {
				Citizen citizen = Citizen.getCitizen(sender);
				if (citizen != null) {
					City city = citizen.getAffiliation();
					if (city != null) {
						if (citizen.isMayor() || citizen.isDeputy()) {
							if (args.length > 1) {
								Citizen target = Citizen.getCitizen(args[2]);
								if (target != null) {
									if (city.isInWaitlist(target)) {
										city.removeWaitlist(target);
										sender.sendMessage(ChatColor.GOLD + target.getName() + ChatColor.BLUE + " has been removed from the waitlist for "
											+ city.getChatName() + ChatColor.BLUE + ". If you want to prevent this Citizen from reapplying, you can ban them"
											+ " with /city ban <Citizen>");
										target.sendMessage(ChatColor.BLUE + "Sorry, your request to join " + city.getChatName() + ChatColor.BLUE 
											+ " has been denied.");
									} else sender.sendMessage(ChatColor.RED + target.getName() + " is not on the waitlist for " + city.getChatName());
								} else sender.sendMessage(Messaging.citizenNotFound(args[2]));
							} else sender.sendMessage(ChatColor.RED + "Not enough arguments. Please specify a player whose join request to accept.");
						} else sender.sendMessage(Messaging.notCityOfficial());
					} else sender.sendMessage(Messaging.noAffiliation());
				} else sender.sendMessage(Messaging.missingCitizenRecord());
			} else sender.sendMessage(Messaging.playersOnly());
		} else sender.sendMessage(Messaging.noPerms("cityzen.city.accept"));
	}
	
	private static void banlist(CommandSender sender, String[] args) {
		City city = null;
		if (args.length <= 2) {
			if (sender.hasPermission("cityzen.city.banlist")) {
				if (sender instanceof Player) {
					Citizen citizen = Citizen.getCitizen(sender);
					if (citizen != null) {
						if (!(citizen.isMayor() || citizen.isDeputy())) {
							sender.sendMessage(Messaging.notCityOfficial());
							return;
						} else {
							city = citizen.getAffiliation();
							if (city == null) {
								sender.sendMessage(Messaging.noAffiliation());
								return;
							}
						}
					} else {
						sender.sendMessage(Messaging.missingCitizenRecord());
						return;
					}
				} else {
					sender.sendMessage(Messaging.playersOnly());
					return;
				}
			} else {
				sender.sendMessage(Messaging.noPerms("cityzen.city.banlist"));
				return;
			}
		} else {
			if (sender.hasPermission("cityzen.city.banlist.others")) {
				city = City.getCity(args[2]);
				if (city == null) {
					sender.sendMessage(Messaging.cityNotFound(args[2]));
					return;
				}
			} else {
				sender.sendMessage(Messaging.noPerms("cityzen.city.banlist.others"));
			}
		}
		if (city != null) {
			
			int pageNumber;
			if (args.length > 1) {
				try {
					pageNumber = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					pageNumber = 1;
				}
			}
			else pageNumber = 1;
			
			List<Citizen> banlist = city.getBanlist();
			
			int numberOfPages = (int) Math.ceil(banlist.size() / 5);
			
			String[][] pages = new String[numberOfPages][5];
			
			for (int page=0;page<numberOfPages;page++) {
				for (int i=0; i < 5;i++) {
					try {
						Citizen banee = banlist.get(page * 5 + i);
						pages[page][i] = banee.getName();
					} catch (IndexOutOfBoundsException e) {
						pages[page][i] = null;
					}
				}
			}
			
			sender.sendMessage(ChatColor.RED + "Citizens banned from " + city.getChatName()
					+ ChatColor.RED + "(" + pageNumber + "/" + numberOfPages);
			sender.sendMessage(pages[pageNumber - 1]);
			if (numberOfPages > 1 && pageNumber < numberOfPages) sender.sendMessage(ChatColor.BLUE + "Type \"/city banlist " 
				+ (pageNumber + 1) + "\" to view the next page.");
		}
	}
	
	private static void distance(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			City city = null;
			if (args.length == 1) {
				if (sender.hasPermission("cityzen.city.distance")) {
					Citizen citizen = Citizen.getCitizen(sender);
					if (citizen != null) {
						city = citizen.getAffiliation();
						if (city == null) {
							sender.sendMessage(Messaging.noAffiliation());
							return;
						}
					} else {
						sender.sendMessage(Messaging.missingCitizenRecord());
						return;
					}
				} else {
					sender.sendMessage(Messaging.noPerms("cityzen.city.distance"));
					return;
				}
			} else {
				if (sender.hasPermission("cityzen.city.distance.others")) {
					city = City.getCity(args[2]);
					if (city == null) {
						sender.sendMessage(Messaging.cityNotFound(args[1]));
						return;
					}
				} else {
					sender.sendMessage(Messaging.noPerms("cityzen.city.distance.others"));
				}
			}
			if (city != null) {
				Location position = ((Player) sender).getLocation();
				Location cityLocation = city.getCenter();
				double x = position.getX();
				double z = position.getZ();
				double cityX = cityLocation.getX();
				double cityZ = cityLocation.getZ();
				double distance = Math.sqrt(Math.pow(x - cityX,2.0) + Math.pow(z - cityZ, 2.0));
				String direction = "";
				if (cityZ - z > 100) direction += "South";
				else if (cityZ - z < -100) direction += "North";
				else direction += "Due ";
				if (cityX - x > 100) direction += "West";
				else if (cityX - x < -100) direction += "East";
				else direction += "ward";
				
				sender.sendMessage(ChatColor.BLUE + "Distance to the center of " + city.getChatName() + ChatColor.BLUE + String.format("(%.2d,%.2d):\n", cityX,cityZ)
						+ String.format("| %.2d Blocks %s of your location",distance,direction));
			}
		} else {
			sender.sendMessage(Messaging.playersOnly());
			return;
		}
	}
	
	private static void visitors(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			City city = null;
			if (args.length == 1) {
				if (sender.hasPermission("cityzen.city.distance")) {
					Citizen citizen = Citizen.getCitizen(sender);
					if (citizen != null) {
						city = citizen.getAffiliation();
						if (city == null) {
							sender.sendMessage(Messaging.noAffiliation());
							return;
						}
					} else {
						sender.sendMessage(Messaging.missingCitizenRecord());
						return;
					}
				} else {
					sender.sendMessage(Messaging.noPerms("cityzen.city.distance"));
					return;
				}
			} else {
				if (sender.hasPermission("cityzen.city.distance.others")) {
					city = City.getCity(args[2]);
					if (city == null) {
						sender.sendMessage(Messaging.cityNotFound(args[1]));
						return;
					}
				} else {
					sender.sendMessage(Messaging.noPerms("cityzen.city.distance.others"));
				}
			}
			if (city != null) {
				Player mayor = null;
				Vector<Player> deputies = new Vector<Player>();
				Vector<Player> citizens = new Vector<Player>();
				Vector<Player> visitors = new Vector<Player>();
				for (Player p : plugin.getServer().getOnlinePlayers()) {
					if (city.isInCity(p.getLocation().getX(), p.getLocation().getZ())) {
						Citizen citizen = Citizen.getCitizen(p);
						if (city.equals(citizen.getAffiliation())) {
							if (citizen.isMayor()) mayor = p;
							else if (citizen.isDeputy()) deputies.add(p);
							else citizens.add(p);
						}
						else visitors.add(p);
					}
				}
				
				if (mayor != null || deputies.size() > 0 || citizens.size() > 0 || visitors.size() > 0) {
					String playerList = "";
					
					sender.sendMessage(ChatColor.BLUE + "Players currently located in " + city.getChatName());
					if (mayor != null) sender.sendMessage(ChatColor.RED + "Mayor: " + ChatColor.RESET + mayor.getDisplayName());
					if (deputies.size() > 0) {
						sender.sendMessage(ChatColor.YELLOW + "Deputies:\n");
						for (Player p : deputies) {
							playerList += p.getDisplayName() + " ";
						}
						sender.sendMessage(playerList);
						playerList = "";
					}
					if (citizens.size() > 0) {
						sender.sendMessage(ChatColor.GREEN + "Citizens:\n");
						for (Player p : citizens) {
							playerList += p.getDisplayName() + " ";
						}
						sender.sendMessage(playerList);
						playerList = "";
					}
					if (visitors.size() > 0) {
						sender.sendMessage(ChatColor.BLUE + "Visitors:\n");
						for (Player p : visitors) {
							playerList += p.getDisplayName() + " ";
						}
						sender.sendMessage(playerList);
						playerList = "";
					}
					
				} else {
					sender.sendMessage(ChatColor.BLUE + "Nobody is in " + city.getChatName());
				}
			}
		} else {
			sender.sendMessage(Messaging.playersOnly());
			return;
		}
	}
}
