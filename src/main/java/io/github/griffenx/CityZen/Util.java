package io.github.griffenx.CityZen;

import java.util.List;
import java.util.Vector;

public class Util {
	public static String collapseArguments(String[] args, int start, int stop) {
		StringBuilder builder = new StringBuilder(args[start]);
		for (int i = start + 1; i < stop && i < args.length; i++) {
			builder.append(" " + args[i]);
		} return builder.toString();
	}
	
	public static String collapseArguments(String[] args, int start) {
		return collapseArguments(args, start, args.length);
	}
	
	public static String collapseArgsWithoutCityName(String[] args, int start, String cityName) {
		int adjustment = cityName.split(" ").length;
		StringBuilder joinedArgs = new StringBuilder(args[1]);
		for (int i = start; i < args.length - adjustment; i++) {
			joinedArgs.append(" " + args[i]);
		} return joinedArgs.toString();
	}
	
	public static String findCityName(String[] args) {
		List<City> citiesMatched = new Vector<>();
		for (int i = args.length - 1; i >= 0; i--) {
			String name = collapseArguments(args, i, args.length);
			City city = City.getCity(name);
			if (city != null) citiesMatched.add(city);
		}
		if (citiesMatched.size() == 0) return null;
		else if (citiesMatched.size() == 1) return citiesMatched.get(0).getName();
		else {
			City mostComplexName = null;
			for (City c : citiesMatched) {
				if (mostComplexName == null || c.getName().split(" ").length > mostComplexName.getName().split(" ").length) mostComplexName = c;
			}
			if (mostComplexName == null) return null;
			else return mostComplexName.getName();
		}
	}
	
	public static double getDistace(Position pos1, Position pos2) {
		return Math.sqrt(Math.pow(pos1.x - pos2.x, 2) + Math.pow(pos1.z - pos2.z, 2));
	}
}