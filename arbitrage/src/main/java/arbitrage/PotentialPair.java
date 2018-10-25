package arbitrage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;

public class PotentialPair {
	
	
	Map<String,Ticker> buy_ticker;
	Map<String,Ticker> sell_ticker;
	double delta_procent;
	CurrencyPair cp;
	BigDecimal min_trade_ammount_buy;
	BigDecimal trade_fee_buy;
	BigDecimal min_trade_ammount_sell;
	BigDecimal trade_fee_sell;
	BigDecimal withdraw_fee_buy;
	BigDecimal withdraw_fee_sell;

	
	public PotentialPair()
	{
		buy_ticker = new HashMap<String,Ticker>();
		sell_ticker = new HashMap<String,Ticker>();
		delta_procent = 0 ;
	}
	public void SetWithdrawBuyFee(BigDecimal fee)
	{
		withdraw_fee_buy = new BigDecimal(fee.doubleValue());
	}
	public void SetWithdrawSellFee(BigDecimal fee)
	{
		withdraw_fee_sell = new BigDecimal(fee.doubleValue());
	}
	public void SetMinTradeAmmountBuy(BigDecimal ammount)
	{
		min_trade_ammount_buy = new BigDecimal(ammount.doubleValue());
	}
	public void SetTradeFeeBuy(BigDecimal fee)
	{
		 trade_fee_buy = new BigDecimal(fee.doubleValue());
	}
	public void SetMinTradeAmmountSell(BigDecimal ammount)
	{
		min_trade_ammount_sell = new BigDecimal(ammount.doubleValue());
	}
	public void SetTradeFeeSell(BigDecimal fee)
	{
		 trade_fee_sell = new BigDecimal(fee.doubleValue());
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
	public void SetCurrencyPair(CurrencyPair pair)
	{
		cp = new CurrencyPair(pair.toString());
	}
	public BigDecimal GetMinTradeAmmountBuy()
	{
		return min_trade_ammount_buy;
	}
	public BigDecimal GetTradeFeeBuy()
	{
		return trade_fee_buy;
	}
	public BigDecimal GetMinTradeAmmountSell()
	{
		return min_trade_ammount_sell;
	}
	public BigDecimal GetTradeFeeSell()
	{
		return trade_fee_sell;
	}
	public BigDecimal GetWithdrawBuyFee()
	{
		return withdraw_fee_buy ;
	}
	public BigDecimal GetWithdrawSellFee()
	{
		return withdraw_fee_sell ;
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
	public String GetBuyExchange()
	{
		Set<String> ks = null;
		String key = null;
		ks = buy_ticker.keySet();
		if ( ks.size() == 1)
		{
			key = (String) ks.toArray()[0];
			return key;
		}
		else
		{
			return null;
		}
	}
	public Ticker GetSellTicker() 
	{
		Set<String> ks = null;
		String key = null;
		ks = sell_ticker.keySet();
		if ( ks.size() == 1)
		{
			key = (String) ks.toArray()[0];
			return sell_ticker.get(key);
		}
		else
			return  null;
	}
	public String GetSellExchange()
	{
		Set<String> ks = null;
		String key = null;
		ks = sell_ticker.keySet();
		if ( ks.size() == 1)
		{
			key = (String) ks.toArray()[0];
			return key;
		}
		else
		{
			return null;
		}
	}	
	public double GetDeltaProcent()
	{
		return delta_procent;
	}
	public CurrencyPair GetCurrencyPair()
	{
		return cp;
	}
	
	public String toString() {
		return cp.toString()+"  BUY  "+GetBuyExchange()+"  "+
					GetBuyTicker().getBid().toString()+"  SELL  "+GetSellExchange()+"  "+GetSellTicker().getAsk().toString()+"  - "
				   +delta_procent+" % "+" buy fee "+trade_fee_buy.doubleValue()+" sell fee "+trade_fee_sell.doubleValue()+" min trade ammount buy"+min_trade_ammount_buy.doubleValue()+
				   " min trade ammount sell "+min_trade_ammount_sell.doubleValue()+" withdraw fee buy "+withdraw_fee_buy.doubleValue()+" - withdraw fee sell "+withdraw_fee_sell.doubleValue() ;
	}

}
