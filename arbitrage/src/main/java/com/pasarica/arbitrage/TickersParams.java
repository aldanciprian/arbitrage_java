package com.pasarica.arbitrage;

import java.util.Collection;
import java.util.List;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;

public class TickersParams implements CurrencyPairsParam {

	public List<CurrencyPair> pairs = null;
	
	public TickersParams(List<CurrencyPair> pairs) {
		// TODO Auto-generated constructor stub
		this.pairs = pairs;
	}
	@Override
	public Collection<CurrencyPair> getCurrencyPairs() {
		// TODO Auto-generated method stub
		return pairs;
	}

}
