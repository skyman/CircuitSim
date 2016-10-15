package com.ra4king.circuitsimulator.components;

import com.ra4king.circuitsimulator.CircuitState;
import com.ra4king.circuitsimulator.WireValue;

/**
 * @author Roi Atalla
 */
public class Constant extends Pin {
	private final WireValue value;
	
	public Constant(String name, WireValue value) {
		super(name, value.getBitSize());
		this.value = new WireValue(value);
	}
	
	@Override
	public void setValue(CircuitState circuitState, WireValue value) {
		this.value.set(value);
		circuitState.getCircuit().getCircuitStates().forEach(state -> state.pushValue(getPort(0), value));
	}
	
	@Override
	public void init(CircuitState circuitState) {
		super.init(circuitState);
		circuitState.pushValue(getPort(0), value);
	}
}