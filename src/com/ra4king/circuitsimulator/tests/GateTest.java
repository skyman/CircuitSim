package com.ra4king.circuitsimulator.tests;

import com.ra4king.circuitsimulator.Circuit;
import com.ra4king.circuitsimulator.Simulator;
import com.ra4king.circuitsimulator.WireValue;
import com.ra4king.circuitsimulator.components.Pin;
import com.ra4king.circuitsimulator.components.gates.AndGate;
import com.ra4king.circuitsimulator.components.gates.OrGate;
import com.ra4king.circuitsimulator.components.gates.XorGate;

/**
 * @author Roi Atalla
 */
public class GateTest {
	public static void main(String[] args) {
		Simulator simulator = new Simulator();
		Circuit circuit = new Circuit(simulator);
		
		AndGate andGate = circuit.addComponent(new AndGate("", 1, 2));
		Pin in1 = circuit.addComponent(new Pin("A", 1));
		Pin in2 = circuit.addComponent(new Pin("B", 1));
		Pin out = circuit.addComponent(new Pin("Out", 1));
		
		andGate.getPort(0).linkPort(in1.getPort(0));
		andGate.getPort(1).linkPort(in2.getPort(0));
		andGate.getPort(2).linkPort(out.getPort(0));
		
		in1.setValue(circuit.getTopLevelState(), WireValue.of(0, 1));
		simulator.stepAll();
		in2.setValue(circuit.getTopLevelState(), WireValue.of(0, 1));
		simulator.stepAll();
		in1.setValue(circuit.getTopLevelState(), WireValue.of(1, 1));
		simulator.stepAll();
		in2.setValue(circuit.getTopLevelState(), WireValue.of(1, 1));
		simulator.stepAll();
		
		XorGate xorGate = circuit.addComponent(new XorGate("", 1, 2));
		OrGate orGate = circuit.addComponent(new OrGate("", 1, 2));
		xorGate.getPort(0).linkPort(in1.getPort(0));
		xorGate.getPort(1).linkPort(orGate.getPort(2));
		orGate.getPort(0).linkPort(xorGate.getPort(2));
		simulator.stepAll();
	}
}
