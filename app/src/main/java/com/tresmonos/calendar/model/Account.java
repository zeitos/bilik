package com.tresmonos.calendar.model;

public class Account {
	private String name;

	public Account(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

    public static String extractDomain(String accountName) {
        String[] parts = accountName.split("[@\\( ]");
        if (parts.length < 2)
            return null;
        return parts[1];
    }
}
