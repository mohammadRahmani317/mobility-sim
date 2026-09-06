document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('simulationForm');
    const presetSelect = document.getElementById('presetSelect');
    const applyPresetBtn = document.getElementById('applyPreset');
    const submitBtn = document.getElementById('submit');
    const schedulingAlgorithm = document.getElementById('schedulingAlgorithm');
    const errorBox = document.getElementById('errorMessage');
    const resultSection = document.getElementById('resultSection');
    const resultBody = document.getElementById('resultBody');
    const lists = {
        datacenters: document.getElementById('datacenterList'),
        hosts: document.getElementById('hostList'),
        vms: document.getElementById('vmList')
    };
    let presets = [];

    const fieldDefinitions = {
        datacenters: [
            {name: 'architecture', label: 'Architecture', required: true},
            {name: 'os', label: 'OS', required: true},
            {name: 'vmm', label: 'VMM', required: true},
            {name: 'timeZone', label: 'Time Zone', type: 'number'},
            {name: 'costPerSec', label: 'Cost Per Sec', type: 'number', min: 0},
            {name: 'costPerMem', label: 'Cost Per Mem', type: 'number', min: 0},
            {name: 'costPerStorage', label: 'Cost Per Storage', type: 'number', min: 0},
            {name: 'costPerBw', label: 'Cost Per BW', type: 'number', min: 0}
        ],
        hosts: [
            {name: 'ram', label: 'RAM (MB)', type: 'number', min: 1},
            {name: 'storage', label: 'Storage', type: 'number', min: 1},
            {name: 'bw', label: 'Bandwidth (Mbit/s)', type: 'number', min: 1},
            {name: 'pes', label: 'PEs', type: 'number', min: 1},
            {name: 'mipsPerPe', label: 'MIPS per PE', type: 'number', min: 1}
        ],
        vms: [
            {name: 'mips', label: 'MIPS', type: 'number', min: 1},
            {name: 'pes', label: 'PEs', type: 'number', min: 1},
            {name: 'ram', label: 'RAM (MB)', type: 'number', min: 1},
            {name: 'bw', label: 'Bandwidth (Mbit/s)', type: 'number', min: 1},
            {name: 'size', label: 'Size', type: 'number', min: 1},
            {name: 'vmm', label: 'VMM', required: true}
        ]
    };

    function createCard(type, data = {}) {
        const card = document.createElement('div');
        card.className = 'card';
        fieldDefinitions[type].forEach(field => {
            const wrapper = document.createElement('label');
            wrapper.className = 'field';
            wrapper.append(document.createTextNode(field.label));
            const input = document.createElement('input');
            input.name = field.name;
            input.type = field.type || 'text';
            input.value = data[field.name] ?? '';
            input.required = field.required !== false;
            if (input.type === 'number') {
                input.step = 'any';
                if (field.min !== undefined) input.min = String(field.min);
            }
            wrapper.appendChild(input);
            card.appendChild(wrapper);
        });
        const remove = document.createElement('button');
        remove.textContent = 'Remove';
        remove.type = 'button';
        remove.className = 'remove-btn';
        remove.addEventListener('click', () => card.remove());
        card.appendChild(remove);
        return card;
    }

    function addResource(type, data = {}) {
        lists[type].appendChild(createCard(type, data));
    }

    function applyPreset(preset) {
        Object.values(lists).forEach(list => list.replaceChildren());
        addResource('datacenters', preset.datacenter);
        addResource('hosts', preset.host);
        addResource('vms', preset.vm);
    }

    async function loadPresets() {
        try {
            const response = await fetch('/api/mobility-sim/presets');
            if (!response.ok) throw new Error(`Preset request failed (${response.status})`);
            presets = await response.json();
            presets.forEach((preset, index) => {
                const option = document.createElement('option');
                option.value = String(index);
                option.textContent = preset.type;
                presetSelect.appendChild(option);
            });
            if (presets.length > 0) {
                presetSelect.value = '0';
                applyPreset(presets[0]);
            }
        } catch (error) {
            showError(`Could not load presets: ${error.message}`);
        }
    }

    function collect(type) {
        return Array.from(lists[type].querySelectorAll('.card')).map(card => {
            const value = {};
            card.querySelectorAll('input').forEach(input => {
                value[input.name] = input.type === 'number' ? Number(input.value) : input.value.trim();
            });
            return value;
        });
    }

    function buildSimulationRequest() {
        return {
            datacenters: collect('datacenters'),
            hosts: collect('hosts'),
            vms: collect('vms'),
            schedulingAlgorithm: schedulingAlgorithm.value
        };
    }

    function validateForm() {
        if (!form.reportValidity()) return false;
        const missing = Object.entries(lists).find(([, list]) => list.children.length === 0);
        if (missing) {
            showError(`Add at least one ${missing[0].replace(/s$/, '')}.`);
            return false;
        }
        return true;
    }

    function setRunning(running) {
        submitBtn.disabled = running;
        submitBtn.textContent = running ? 'Running…' : 'Run Simulation';
    }

    function showError(message) {
        errorBox.textContent = message;
        errorBox.hidden = false;
    }

    function clearMessages() {
        errorBox.hidden = true;
        resultSection.hidden = true;
    }

    function showResult(result) {
        const rows = [
            ['Scheduling Algorithm', result.schedulingAlgorithm],
            ['Total Workflows', result.totalWorkflowCount],
            ['Successful Workflows', result.successfulWorkflowCount],
            ['Failed Workflows', result.failedWorkflowCount],
            ['Total Tasks', result.totalTaskCount],
            ['Completed Tasks', result.completedTaskCount],
            ['Failed Tasks', result.failedTaskCount],
            ['Deadline Success Rate', `${formatNumber(result.deadlineSuccessRate)}%`],
            ['Makespan', `${formatNumber(result.makespan)} sec`],
            ['Average Waiting Time', `${formatNumber(result.averageWaitingTime)} sec`]
        ];
        resultBody.replaceChildren(...rows
            .filter(([, value]) => value !== null && value !== undefined)
            .map(([label, value]) => {
                const row = document.createElement('tr');
                const heading = document.createElement('th');
                const cell = document.createElement('td');
                heading.textContent = label;
                cell.textContent = String(value);
                row.append(heading, cell);
                return row;
            }));
        resultSection.hidden = false;
        resultSection.scrollIntoView({behavior: 'smooth', block: 'start'});
    }

    function formatNumber(value) {
        return Number.isFinite(Number(value)) ? Number(value).toFixed(2) : value;
    }

    async function runSimulation() {
        clearMessages();
        if (!validateForm()) return;
        setRunning(true);
        try {
            const response = await fetch('/api/mobility-sim', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(buildSimulationRequest())
            });
            const body = await response.json().catch(() => null);
            if (!response.ok) {
                throw new Error(body?.message || body?.error || `Simulation failed (${response.status})`);
            }
            showResult(body);
        } catch (error) {
            showError(error.message || 'Simulation failed.');
        } finally {
            setRunning(false);
        }
    }

    document.getElementById('addDatacenter').addEventListener('click', () => addResource('datacenters'));
    document.getElementById('addHost').addEventListener('click', () => addResource('hosts'));
    document.getElementById('addVm').addEventListener('click', () => addResource('vms'));
    applyPresetBtn.addEventListener('click', () => {
        const preset = presets[Number(presetSelect.value)];
        if (preset) applyPreset(preset);
    });
    form.addEventListener('submit', event => {
        event.preventDefault();
        runSimulation();
    });

    loadPresets();
});
