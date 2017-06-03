<h1>Hibernate Inspections Plugin for IntelliJ IDEA</h1>

Hibernate silently fails in certain situations, leading to bugs which are difficult to track down.
This plugin helps finding and fixing some of these problems.
<br>

In IntelliJ IDEA, under <b>Settings > Inspections > Hibernate inspections</b> it adds the following inspections:
      <ul>
        <li>Persisted class is final</li>
        <li>Final method of a persisted class uses direct field access</li>
        <li>Embeddable subclasses embeddable</li>
      </ul>

To install it, in IntelliJ IDEA go to <b>Settings > Plugins > Browse repositories</b> and select <b>Hibernate Inspections</b>.
