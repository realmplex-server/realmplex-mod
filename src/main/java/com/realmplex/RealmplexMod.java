package com.realmplex;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmplexMod implements ModInitializer {
	public static final String MOD_ID = "realmplex-mod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Loaded Realmplex Mod");
		CurrencyConverter.register();
	}
}