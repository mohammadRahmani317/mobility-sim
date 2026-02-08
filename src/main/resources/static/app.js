document.addEventListener('DOMContentLoaded', () => {
    const presetSelect = document.getElementById('presetSelect');
    const applyPresetBtn = document.getElementById('applyPreset');
    const submitBtn = document.getElementById('submit');

    const datacenterList = document.getElementById('datacenterList');
    const hostList = document.getElementById('hostList');
    const vmList = document.getElementById('vmList');

    const addDatacenterBtn = document.getElementById('addDatacenter');
    const addHostBtn = document.getElementById('addHost');
    const addVmBtn = document.getElementById('addVm');

    let presets = {};

    // ---------------- Helpers ----------------
    function createInput(name, placeholder, type = "text", value = "") {
        const input = document.createElement("input");
        input.name = name;
        input.placeholder = placeholder;
        input.type = type;
        input.value = value;
        input.title=name;
        return input;
    }

    function removeButton(container) {
        const btn = document.createElement("button");
        btn.textContent = "Remove";
        btn.type = "button";
        btn.onclick = () => container.remove();
        return btn;
    }

    function createCard(fields, data = {}) {
        const card = document.createElement("div");
        card.className = "card";
        fields.forEach(f => {
            card.appendChild(createInput(f.name, f.placeholder, f.type || "text", data[f.name] || ""));
        });
        card.appendChild(removeButton(card));
        return card;
    }

    // ---------------- Add Resource Functions ----------------
    function addDatacenter(data = {}) {
        const fields = [
            {name: "architecture", placeholder: "Architecture"},
            {name: "os", placeholder: "OS"},
            {name: "vmm", placeholder: "VMM"},
            {name: "timeZone", placeholder: "Time Zone", type: "number"},
            {name: "costPerSec", placeholder: "Cost Per Sec", type: "number"},
            {name: "costPerMem", placeholder: "Cost Per Mem", type: "number"},
            {name: "costPerStorage", placeholder: "Cost Per Storage", type: "number"},
            {name: "costPerBw", placeholder: "Cost Per BW", type: "number"}
        ];
        const card = createCard(fields, data);
        datacenterList.appendChild(card);
    }

    function addHost(data = {}) {
        const fields = [
            {name: "ram", placeholder: "RAM", type: "number"},
            {name: "storage", placeholder: "Storage", type: "number"},
            {name: "bw", placeholder: "Bandwidth", type: "number"},
            {name: "pes", placeholder: "PEs", type: "number"},
            {name: "mipsPerPe", placeholder: "MIPS per PE", type: "number"}
        ];
        const card = createCard(fields, data);
        hostList.appendChild(card);
    }

    function addVm(data = {}) {
        const fields = [
            {name: "mips", placeholder: "MIPS", type: "number"},
            {name: "pes", placeholder: "PEs", type: "number"},
            {name: "ram", placeholder: "RAM", type: "number"},
            {name: "bw", placeholder: "Bandwidth", type: "number"},
            {name: "size", placeholder: "Size", type: "number"},
            {name: "vmm", placeholder: "VMM"}
        ];
        const card = createCard(fields, data);
        vmList.appendChild(card);
    }

    // ---------------- Fetch Presets from Backend ----------------
    fetch('/api/presets')
        .then(res => res.json())
        .then(data => {
            presets = data; // expects { "LINUX": {...}, "WINDOWS": {...} }
            Object.keys(presets).forEach(key => {
                const option = document.createElement("option");
                option.value = key;
                option.textContent = presets[key].type;
                presetSelect.appendChild(option);
            });
        })
        .catch(err => console.error("Failed to load presets:", err));

    // ---------------- Apply Preset ----------------
    applyPresetBtn.addEventListener('click', () => {
        const type = presetSelect.value;
        if (!type || !presets[type]) return;

        const preset = presets[type];

        // Clear existing lists
        datacenterList.innerHTML = "";
        hostList.innerHTML = "";
        vmList.innerHTML = "";

        // Add preset as first card
        addDatacenter(preset.datacenter);
        addHost(preset.host);
        addVm(preset.vm);
    });

    // ---------------- Add Buttons ----------------
    addDatacenterBtn.addEventListener('click', () => addDatacenter());
    addHostBtn.addEventListener('click', () => addHost());
    addVmBtn.addEventListener('click', () => addVm());

    // ---------------- Submit ----------------
    submitBtn.addEventListener('click', () => {
        function collect(list) {
            return Array.from(list.children).map(card => {
                const obj = {};
                card.querySelectorAll('input').forEach(input => {
                    obj[input.name] = input.type === "number" ? Number(input.value) : input.value;
                });
                return obj;
            });
        }

        const payload = {
            datacenters: collect(datacenterList),
            hosts: collect(hostList),
            vms: collect(vmList)
        };

        fetch('/api/cloudsim', {
            method: 'POST',
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        })
            .then(res => res.json())
            .then(resp => {
                alert("Data submitted successfully!");
                console.log(resp);
            })
            .catch(err => {
                alert("Failed to submit data!");
                console.error(err);
            });
    });
});
