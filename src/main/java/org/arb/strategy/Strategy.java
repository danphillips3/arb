package org.arb.strategy;

import java.util.ArrayList;
import org.arb.CalcPartition;
import org.arb.TradeDetails;

public interface Strategy {
	public ArrayList<TradeDetails> getTrades(CalcPartition partition);
}
