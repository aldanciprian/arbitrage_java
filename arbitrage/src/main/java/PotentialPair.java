package main.java;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knowm.xchange.dto.marketdata.Ticker;

public class PotentialPair {
	
	
	Map<String,Ticker> buy_ticker;
	Map<String,Ticker> sell_ticker;
	double delta_procent;
	
	public PotentialPair()
	{
		buy_ticker = new HashMap<String,Ticker>();
		sell_ticker = new HashMap<String,Ticker>();
		delta_procent = 0 ;
	}
	
	public void SetBuyTicker(String exchange,Ticker tick)
	{
		buy_ticker.put(exchange, tick);
	}
	public void SetSellTicker(String exchange,Ticker tick)
	{
		sell_ticker.put(exchange, tick);
	}
	public void SetDeltaProcent(double proc)
	{
		delta_procent = proc;
	}
	public Ticker GetBuyTicker() 
	{
		Set<String> ks = null;
		String key = null;
		ks = buy_ticker.keySet();
		if ( ks.size() == 1)
		{
			key = (String) ks.toArray()[0];
			return buy_ticker.get(key);
		}
		else
			return  null;
	}

}
