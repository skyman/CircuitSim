package com.ra4king.circuitsim.simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ra4king.circuitsim.simulator.Port.Link;

import javafx.util.Pair;

/**
 * @author Roi Atalla
 */
public class Simulator {
	private Set<Circuit> circuits;
	private Collection<Pair<CircuitState, Link>> linksToUpdate, temp, shortCircuited;
	private ShortCircuitException lastShortCircuit;
	private final Set<Collection<Pair<CircuitState, Link>>> history;
	
	// Create a Lock with a fair policy
	private final ReentrantLock lock = new ReentrantLock(true);
	
	public Simulator() {
		circuits = new HashSet<>();
		linksToUpdate = new LinkedHashSet<>();
		temp = new LinkedHashSet<>();
		shortCircuited = new ArrayList<>();
		history = new HashSet<>();
	}
	
	/**
	 * Get the ReentrantLock instance
	 *
	 * @return The ReentrantLock instance used for synchronization
	 */
	public Lock getLock() {
		return lock;
	}
	
	/**
	 * Allows execution of code that is synchronized with the Simulator
	 *
	 * Similar to but more efficient than <code>synchronized(simulator) { runnable.run(); }</code>
	 *
	 * @param runnable The block of code to run synchronously
	 */
	public void runSync(Runnable runnable) {
		lock.lock();
		
		try {
			runnable.run();
		} finally {
			lock.unlock();
		}
	}
	
	public Collection<Pair<CircuitState, Link>> getLinksToUpdate() {
		return linksToUpdate;
	}
	
	private boolean tmp;
	
	public boolean hasLinksToUpdate() {
		runSync(() -> tmp = !linksToUpdate.isEmpty());
		return tmp;
	}
	
	/**
	 * Clears all circuits and queue of un-propagated links.
	 */
	public void clear() {
		circuits.clear();
		linksToUpdate.clear();
		temp.clear();
		shortCircuited.clear();
		history.clear();
	}
	
	/**
	 * Resets all CircuitStates of all attached Circuits.
	 */
	public void reset() {
		runSync(() -> circuits.stream().flatMap(
				circuit -> circuit.getCircuitStates().stream()).forEach(CircuitState::reset));
	}
	
	public Set<Circuit> getCircuits() {
		return circuits;
	}
	
	/**
	 * Add the Circuit to this Simulator.
	 *
	 * @param circuit The Circuit to be added.
	 */
	public void addCircuit(Circuit circuit) {
		runSync(() -> circuits.add(circuit));
	}
	
	/**
	 * Remove the Circuit from this Simulator.
	 *
	 * @param circuit The Circuit to be removed.
	 */
	public void removeCircuit(Circuit circuit) {
		runSync(() -> circuits.remove(circuit));
	}
	
	/**
	 * Notify the Simulator the specified port has pushed a new value.
	 *
	 * @param state The CircuitState in which the Port has pushed the new value.
	 * @param port  The Port that pushed the new value.
	 */
	public void valueChanged(CircuitState state, Port port) {
		valueChanged(state, port.getLink());
	}
	
	/**
	 * Notify the Simulator the specified Link has received new values.
	 *
	 * @param state The CircuitState in which the Link has received new values.
	 * @param link  The Link that has received new values.
	 */
	public void valueChanged(CircuitState state, Link link) {
		runSync(() -> linksToUpdate.add(new Pair<>(state, link)));
	}
	
	private boolean stepping = false;
	
	/**
	 * Perform only a single propagation step. This is thread-safe.
	 */
	public void step() {
		runSync(() -> {
			if(stepping) {
				return;
			}
			
			try {
				stepping = true;
				
				Collection<Pair<CircuitState, Link>> tmp = linksToUpdate;
				linksToUpdate = temp;
				temp = tmp;
				
				temp.addAll(shortCircuited);
				
				linksToUpdate.clear();
				shortCircuited.clear();
				lastShortCircuit = null;
				
				temp.forEach(pair -> {
					try {
						pair.getKey().propagateSignal(pair.getValue());
					} catch(ShortCircuitException exc) {
						shortCircuited.add(pair);
						lastShortCircuit = exc;
					}
				});
				
				if(lastShortCircuit != null && linksToUpdate.size() == 0) {
					throw lastShortCircuit;
				}
				
				linksToUpdate.addAll(shortCircuited);
			} finally {
				stepping = false;
			}
		});
	}
	
	/**
	 * Continuously steps the simulation until no more propagation is needed. This is thread-safe.
	 */
	public void stepAll() {
		runSync(() -> {
			if(stepping) {
				return;
			}
			
			history.clear();
			
			do {
				if(history.contains(linksToUpdate)) {
					linksToUpdate.clear();
					throw new IllegalStateException("Oscillation apparent.");
				}
				
				history.add(new LinkedHashSet<>(linksToUpdate));
				step();
			} while(!linksToUpdate.isEmpty());
		});
	}
}
